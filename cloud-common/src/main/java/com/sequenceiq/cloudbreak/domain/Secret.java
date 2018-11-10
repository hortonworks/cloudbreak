package com.sequenceiq.cloudbreak.domain;

import java.io.Serializable;

public class Secret implements Serializable {

    public static final Secret EMPTY = new Secret();

    private final String raw;

    private final String secret;

    private Secret() {
        raw = null;
        secret = null;
    }

    public Secret(String raw) {
        this.raw = raw;
        secret = null;
    }

    public Secret(String raw, String secret) {
        this.raw = raw;
        this.secret = secret;
    }

    public String getRaw() {
        return raw;
    }

    public String getSecret() {
        return secret;
    }
}
