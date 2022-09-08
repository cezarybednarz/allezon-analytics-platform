package com.allezon;

import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WebController {

    private final Map<String, UserTag> user_tags;
    Logger logger = LoggerFactory.getLogger(WebController.class);

    public WebController() {
        this.user_tags = new HashMap<>();
        logger.info("Log level: INFO");
    }

    @PostMapping(value = "/user_tags", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveUserTag(@RequestBody UserTag user_tag) {
        logger.info(user_tag.getCookie());
        this.user_tags.put(user_tag.getCookie(), user_tag);

    }

}

