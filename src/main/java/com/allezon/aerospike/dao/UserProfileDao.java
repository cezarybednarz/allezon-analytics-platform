package com.allezon.aerospike.dao;

import com.aerospike.client.*;
import com.aerospike.client.policy.*;
import com.allezon.aerospike.UserProfile;
import com.allezon.aerospike.UserTag;
import com.allezon.aerospike.avro.SerDe;
import com.allezon.aerospike.schema.SchemaVersion;
import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.schema.registry.client.SchemaRegistryClient;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class UserProfileDao {
    private static final String NAMESPACE = "mimuw";
    private static final String SET = "userprofiles";
    private static final String USER_PROFILE_BIN = "userprofile";
    private static final String VERSION_BIN = "version";

    private AerospikeClient client;
    private SerDe<UserProfile> serde;

    @Autowired
    private SchemaVersion schemaVersion;

    @Autowired
    private SchemaRegistryClient schemaRegistryClient;

    private static ClientPolicy defaultClientPolicy() {
        ClientPolicy defaulClientPolicy = new ClientPolicy();
        defaulClientPolicy.readPolicyDefault.replica = Replica.MASTER_PROLES;
        defaulClientPolicy.readPolicyDefault.socketTimeout = 100;
        defaulClientPolicy.readPolicyDefault.totalTimeout = 100;
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

    private UserProfile updateUserProfile(UserProfile userProfile, UserTag userTag) {
        if(userProfile == null) {
            userProfile = new UserProfile(userTag.getCookie().toString(), new ArrayList<>(), new ArrayList<>());
        }
        if(userTag.getAction().equals("VIEW")) {
            List<UserTag> views = userProfile.getViews();
            views.add(userTag);
            userProfile.setViews(views);
        } else {
            List<UserTag> buys = userProfile.getBuys();
            buys.add(userTag);
            userProfile.setBuys(buys);
        }
        return userProfile;
    }

    public void put(UserTag userTag) {
        String cookie = userTag.getCookie().toString();

        WritePolicy writePolicy = new WritePolicy(client.writePolicyDefault);
        UserProfile userProfile = this.get(cookie);

        UserProfile updatedUserProfile = updateUserProfile(userProfile, userTag);
        Key key = new Key(NAMESPACE, SET, cookie);
        Bin versionBin = new Bin(VERSION_BIN, schemaVersion.getCurrentSchemaVersion());
        Bin userProfileBin = new Bin(USER_PROFILE_BIN, serde.serialize(updatedUserProfile));

        client.put(writePolicy, key, versionBin, userProfileBin);
    }

    public UserProfile get(String cookie) {
        Policy readPolicy = new Policy(client.readPolicyDefault);

        Key key = new Key(NAMESPACE, SET, cookie);
        Record record = client.get(readPolicy, key, VERSION_BIN, USER_PROFILE_BIN);

        if (record == null) {
            return null;
        }

        int writerSchemaId = record.getInt(VERSION_BIN);
        if (schemaVersion.getCurrentSchemaVersion() == writerSchemaId) {
            return serde.deserialize((byte[]) record.getValue(USER_PROFILE_BIN), UserProfile.getClassSchema());
        }

        String schema = schemaRegistryClient.fetch(writerSchemaId);
        if (schema == null) {
            throw new IllegalStateException("Schema with id: " + writerSchemaId + " does not exist");
        }

        return serde.deserialize((byte[]) record.getValue(USER_PROFILE_BIN), new Schema.Parser().parse(schema));
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
