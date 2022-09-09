package com.allezon;

import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        if (!this.user_tags.containsKey(user_tag.getCookie())) {
            this.user_tags.put(user_tag.getCookie(), new ArrayList<>(List.of(user_tag)));
        } else {
            this.user_tags.get(user_tag.getCookie()).add(user_tag);
        }
    }

    @PostMapping(value = "/user_profiles/{cookie}")
    @ResponseBody
    public UserProfile userProfiles(@PathVariable String cookie,
                                    @RequestParam("time_range") String time_range,
                                    @RequestParam(name = "limit", defaultValue = "200", required = false) String limit,
                                    @RequestBody UserProfile debug) {

        Integer limit_int = Integer.valueOf(limit);

        List<UserTag> cookie_user_tags = this.user_tags.get(cookie);

        if(cookie_user_tags == null) {
            return new UserProfile(cookie, new ArrayList<>(), new ArrayList<>());
        }

        List<UserTag> user_tags_in_range =
                cookie_user_tags.stream()
                        .filter(user_tag -> inRange(user_tag.getTime(), time_range))
                        .collect(Collectors.toList());

        List<UserTag> views =
                user_tags_in_range.stream()
                        .filter(user_tag -> user_tag.getAction().equals("VIEW"))
                        .skip(max(cookie_user_tags.size() - limit_int, 0))
                        .collect(Collectors.toList());

        List<UserTag> buys = user_tags_in_range.stream()
                .filter(user_tag -> user_tag.getAction().equals("BUY"))
                .skip(max(cookie_user_tags.size() - limit_int, 0))
                .collect(Collectors.toList());

        Collections.reverse(views);
        Collections.reverse(buys);

        UserProfile result = new UserProfile(cookie, views, buys);

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

