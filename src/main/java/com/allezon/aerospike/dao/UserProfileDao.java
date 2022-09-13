package com.allezon.aerospike.dao;

import com.aerospike.client.*;
import com.aerospike.client.policy.*;
import com.allezon.aerospike.UserProfile;
import com.allezon.aerospike.UserTag;
import com.allezon.aerospike.avro.SerDe;
import com.allezon.aerospike.schema.SchemaVersion;
import com.allezon.server.WebController;
import org.apache.avro.Schema;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.schema.registry.client.SchemaRegistryClient;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class UserProfileDao {
    private static final String NAMESPACE = "mimuw";
    private static final String SET = "userprofiles";
    private static final String USER_PROFILE_BIN = "userprofile";
    private static final String VERSION_BIN = "version";

    private static final int MAX_USER_TAGS = 200;
    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    private final AerospikeClient client;
    private final SerDe<UserProfile> serde;

    @Autowired
    private SchemaVersion schemaVersion;

    @Autowired
    private SchemaRegistryClient schemaRegistryClient;

    private static ClientPolicy defaultClientPolicy() {
        ClientPolicy defaulClientPolicy = new ClientPolicy();
        defaulClientPolicy.readPolicyDefault.replica = Replica.MASTER_PROLES;
        defaulClientPolicy.readPolicyDefault.socketTimeout = 200;
        defaulClientPolicy.readPolicyDefault.totalTimeout = 200;
        defaulClientPolicy.writePolicyDefault.socketTimeout = 15000;
        defaulClientPolicy.writePolicyDefault.totalTimeout = 15000;
        defaulClientPolicy.writePolicyDefault.maxRetries = 3;
        defaulClientPolicy.writePolicyDefault.commitLevel = CommitLevel.COMMIT_MASTER;
        defaulClientPolicy.writePolicyDefault.recordExistsAction = RecordExistsAction.REPLACE;
        return defaulClientPolicy;
    }

    public UserProfileDao(@Value("${aerospike.seeds}") String[] aerospikeSeeds, @Value("${aerospike.port}") int port) {
        this.client = new AerospikeClient(defaultClientPolicy(), Arrays.stream(aerospikeSeeds).map(seed -> new Host(seed, port)).toArray(Host[]::new));
        this.serde = new SerDe<>(UserProfile.getClassSchema());
    }

    private void sortAndTruncate(List<UserTag> userTags) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat formatterShort = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        userTags.sort(Comparator.comparing(ut -> {
            String time = ut.getTime().toString();
            try {
                return time.length() == 20
                        ? formatterShort.parse(time)
                        : formatter.parse(time);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }));
        if(userTags.size() > MAX_USER_TAGS) {
            userTags.remove(0);
        }
    }
    private UserProfile updateUserProfile(UserProfile userProfile, UserTag userTag) {
        if(userProfile == null) {
            userProfile = new UserProfile(userTag.getCookie().toString(), new ArrayList<>(), new ArrayList<>());
        }

        if(userTag.getAction().equals("VIEW")) {
            List<UserTag> views = userProfile.getViews();
            views.add(userTag);
            sortAndTruncate(views);
            userProfile.setViews(views);
        } else {
            List<UserTag> buys = userProfile.getBuys();
            buys.add(userTag);
            sortAndTruncate(buys);
            userProfile.setBuys(buys);
        }
        return userProfile;
    }

    public void put(UserTag userTag) {
        String cookie = userTag.getCookie().toString();

        for(int attempt = 0; attempt < 3; attempt++) {
            try {
                Pair<UserProfile, Record> userProfileWithGeneration = this.getWithRecord(cookie);
                UserProfile userProfile = userProfileWithGeneration.getLeft();
                Record record = userProfileWithGeneration.getRight();

                UserProfile updatedUserProfile = updateUserProfile(userProfile, userTag);
                Key key = new Key(NAMESPACE, SET, cookie);
                Bin versionBin = new Bin(VERSION_BIN, schemaVersion.getCurrentSchemaVersion());
                Bin userProfileBin = new Bin(USER_PROFILE_BIN, Snappy.compress(serde.serialize(updatedUserProfile)));

                WritePolicy writePolicy = new WritePolicy(client.writePolicyDefault);
                writePolicy.generation = record == null ? 0 : record.generation;
                writePolicy.generationPolicy = GenerationPolicy.EXPECT_GEN_EQUAL;

                client.put(writePolicy, key, versionBin, userProfileBin);
                break;
            } catch (AerospikeException e) {
                // if the record version did not match, it means that it has been changed and we must retry the whole operation
                if (e.getResultCode() == ResultCode.GENERATION_ERROR) {
                    logger.warn("Generation error while trying to update the profile for cookie: {}, attempt: {} - retrying",
                            userTag.getCookie(), attempt + 1);
                    continue;
                }
                throw e;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Pair<UserProfile, Record> getWithRecord(String cookie) {
        Policy readPolicy = new Policy(client.readPolicyDefault);

        Key key = new Key(NAMESPACE, SET, cookie);
        Record record = client.get(readPolicy, key, VERSION_BIN, USER_PROFILE_BIN);

        if (record == null) {
            return Pair.of(null, null);
        }

        int writerSchemaId = record.getInt(VERSION_BIN);
        if (schemaVersion.getCurrentSchemaVersion() == writerSchemaId) {
            try {
                return Pair.of(serde.deserialize(Snappy.uncompress((byte[]) record.getValue(USER_PROFILE_BIN)), UserProfile.getClassSchema()),
                        record);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String schema = schemaRegistryClient.fetch(writerSchemaId);
        if (schema == null) {
            throw new IllegalStateException("Schema with id: " + writerSchemaId + " does not exist");
        }

        try {
            return Pair.of(serde.deserialize(Snappy.uncompress((byte[]) record.getValue(USER_PROFILE_BIN)), new Schema.Parser().parse(schema)),
                    record);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UserProfile get(String cookie) {
        return getWithRecord(cookie).getLeft();
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
