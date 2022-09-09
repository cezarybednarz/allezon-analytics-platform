package com.allezon.server;

import java.util.List;

public class UserProfile {
    private final String cookie;
    private final List<UserTag> views;
    private final List<UserTag> buys;

    public UserProfile(String cookie, List<UserTag> views, List<UserTag> buys) {
        this.cookie = cookie;
        this.views = views;
        this.buys = buys;
    }

    public String getCookie() {
        return this.cookie;
    }

    public List<UserTag> getViews() {
        return this.views;
    }

    public List<UserTag> getBuys() {
        return this.buys;
    }
}
