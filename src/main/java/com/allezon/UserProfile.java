package com.allezon;

import java.util.List;

public class UserProfile {
    private String cookie;
    private List<UserTag> views;
    private List<UserTag> buys;

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
