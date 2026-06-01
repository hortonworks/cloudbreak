package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

public class FreeIpaCertMongerConfigView {
    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String enrollTtls;

    private FreeIpaCertMongerConfigView(Builder builder) {
        this.enrollTtls = builder.enrollTtls;
    }

    public String getEnrollTtls() {
        return ObjectUtils.getIfNull(enrollTtls, EMPTY_CONFIG_DEFAULT);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enroll_ttls", getEnrollTtls());
        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String enrollTtls;

        private Builder() {
        }

        public Builder withEnrollTtls(String enrollTtls) {
            this.enrollTtls = enrollTtls;
            return this;
        }

        public FreeIpaCertMongerConfigView build() {
            return new FreeIpaCertMongerConfigView(this);
        }
    }
}
