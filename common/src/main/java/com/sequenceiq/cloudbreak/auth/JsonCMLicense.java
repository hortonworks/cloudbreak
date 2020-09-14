package com.sequenceiq.cloudbreak.auth;

import java.util.StringJoiner;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonCMLicense {

    private static final int FIRST_PW_CHAR = 0;

    private static final int LAST_PW_CHAR = 12;

    private String name;

    private String uuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPaywallUsername() {
        return uuid;
    }

    public String getPaywallPassword() {
        String hash = DigestUtils.sha256Hex(name + uuid);
        return hash.substring(FIRST_PW_CHAR, LAST_PW_CHAR);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JsonCMLicense.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("uuid=" + uuid)
                .toString();
    }
}
