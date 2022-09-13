package com.allezon.server;

import com.allezon.aerospike.UserProfile;
import com.allezon.aerospike.UserTag;
import com.allezon.aerospike.avro2json.AvroJsonHttpMessageConverter;
import com.allezon.aerospike.dao.UserProfileDao;
import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.schema.registry.avro.AvroSchemaMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.allezon.aerospike.avro2json.AvroJsonHttpMessageConverter.AVRO_JSON;
import static java.lang.Integer.max;

@RestController
public class WebController {

    @Autowired
    private UserProfileDao userProfileDao;
    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    public WebController() { }

    @PostMapping(value = "/user_tags", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void userTags(@RequestBody UserTag userTag) {
        if (userTag.getCookie().toString().equals("Om9a1dPLrwLrTrXPCH3c")) {
            logger.info("time for cookie Om9a1dPLrwLrTrXPCH3c: {}", userTag.getTime().toString());
            logger.info("{}", userTag.getTime().toString().length());
        }
        userProfileDao.put(userTag);
    }

    @PostMapping(produces = AVRO_JSON, value = "/user_profiles/{cookie}")
    @ResponseBody
    public UserProfile userProfiles(@PathVariable String cookie,
                                    @RequestParam("time_range") String time_range,
                                    @RequestParam(name = "limit", defaultValue = "200", required = false) String limit,
                                    @RequestBody UserProfile debug) {
        int limit_int = Integer.parseInt(limit);

        UserProfile dbUserProfile = userProfileDao.get(cookie);

        if(dbUserProfile == null) {
            return new UserProfile(cookie, new ArrayList<>(), new ArrayList<>());
        }

        List<UserTag> dbBuys = dbUserProfile.getBuys();
        List<UserTag> filteredBuys = dbBuys.stream()
                .filter(user_tag -> inRange(user_tag.getTime().toString(), time_range))
                .skip(max(dbBuys.size() - limit_int, 0))
                .collect(Collectors.toList());

        List<UserTag> dbViews = dbUserProfile.getViews();
        List<UserTag> filteredViews = dbViews.stream()
                .filter(user_tag -> inRange(user_tag.getTime().toString(), time_range))
                .skip(max(dbViews.size() - limit_int, 0))
                .collect(Collectors.toList());

        Collections.reverse(filteredViews);
        Collections.reverse(filteredBuys);

        UserProfile result = new UserProfile(cookie, filteredViews, filteredBuys);

        if(!debug.toString().equals(result.toString())) {
            logger.error("different value than debug! {} {}", debug, result);
        }
        return result;
    }

    private boolean inRange(String time, String time_range)  {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat formatterShort = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String[] date_range = time_range.split("_");
        try {
            Date user_tag_date = time.length() == 20
                    ? formatterShort.parse(time.substring(0, time.length() - 1))
                    : formatter.parse(time.substring(0, time.length() - 1));
            Date begin_date = formatter.parse(date_range[0]);
            Date end_date = formatter.parse(date_range[1]);
            return user_tag_date.compareTo(begin_date) >= 0 && user_tag_date.compareTo(end_date) < 0;
        } catch (ParseException e) {
            logger.error("inRange({}, {})", time, time_range);
            throw new RuntimeException(e);
        }
    }
}

