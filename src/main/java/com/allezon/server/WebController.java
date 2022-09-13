package com.allezon.server;

import com.allezon.aerospike.UserProfile;
import com.allezon.aerospike.UserTag;
import com.allezon.aerospike.avro2json.AvroJsonHttpMessageConverter;
import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Map<String, List<UserTag>> user_tags;
    Logger logger = LoggerFactory.getLogger(WebController.class);

    public WebController() {
        this.user_tags = new HashMap<>();
    }

    @PostMapping(value = "/user_tags", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void userTags(@RequestBody UserTag user_tag) {
        if (!this.user_tags.containsKey(user_tag.getCookie().toString())) {
            this.user_tags.put(user_tag.getCookie().toString(), new ArrayList<>(List.of(user_tag)));
        } else {
            this.user_tags.get(user_tag.getCookie().toString()).add(user_tag);
        }
    }

    @PostMapping(produces = AVRO_JSON, value = "/user_profiles/{cookie}")
    @ResponseBody
    public UserProfile userProfiles(@PathVariable String cookie,
                                                    @RequestParam("time_range") String time_range,
                                                    @RequestParam(name = "limit", defaultValue = "200", required = false) String limit,
                                                    @RequestBody UserProfile debug) throws IOException {

        //logger.warn("debug: {} time_range: {}, limit: {}", debug, time_range, limit);
        int limit_int = Integer.parseInt(limit);

        List<UserTag> cookie_user_tags = this.user_tags.get(cookie);
        UserProfile result = new UserProfile(cookie, new ArrayList<>(), new ArrayList<>());
        if(cookie_user_tags == null) {
            return result;
        }

        List<UserTag> user_tags_in_range =
                cookie_user_tags.stream()
                        .filter(user_tag -> inRange(user_tag.getTime().toString(), time_range))
                        .collect(Collectors.toList());

        List<UserTag> views =
                user_tags_in_range.stream()
                        .filter(user_tag -> user_tag.getAction().toString().equals("VIEW"))
                        .skip(max(cookie_user_tags.size() - limit_int, 0))
                        .collect(Collectors.toList());

        List<UserTag> buys = user_tags_in_range.stream()
                .filter(user_tag -> user_tag.getAction().toString().equals("BUY"))
                .skip(max(cookie_user_tags.size() - limit_int, 0))
                .collect(Collectors.toList());

        Collections.reverse(views);
        Collections.reverse(buys);

        result = new UserProfile(cookie, views, buys);

        if(!result.getViews().toString().equals(debug.getViews().toString())) {
            logger.error(time_range);
            logger.error("wrong views!");
            logger.error(String.valueOf(result.getViews()));
            logger.error(String.valueOf(debug.getViews()));
        }
        if(!result.getBuys().toString().equals(debug.getBuys().toString())) {
            logger.error("wrong buys!");
            logger.error(String.valueOf(result.getBuys()));
            logger.error(String.valueOf(debug.getBuys()));
        }
        if(!debug.toString().equals(result.toString())) {
            logger.error("different value than debug! {} {}", debug, result);
        }

        return result;
    }

    private boolean inRange(String time, String time_range)  {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String[] date_range = time_range.split("_");
        try {
            Date user_tag_date = formatter.parse(time.substring(0, time.length() - 1));
            Date begin_date = formatter.parse(date_range[0]);
            Date end_date = formatter.parse(date_range[1]);
            return user_tag_date.compareTo(begin_date) >= 0 && user_tag_date.compareTo(end_date) < 0;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

