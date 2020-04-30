package com.sequenceiq.freeipa.client;

import java.util.Optional;

public class CookieAndStickyId {

    private String cookie;

    private Optional<String> stickyId;

    CookieAndStickyId(String cookie, Optional<String> stickyId) {
        this.cookie = cookie;
        this.stickyId = stickyId;
    }

    public String getCookie() {
        return cookie;
    }

    public Optional<String> getStickyId() {
        return stickyId;
    }
}
