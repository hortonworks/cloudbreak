package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

public class FluentClusterDetails {

    public static final String CLUSTER_CRN_KEY = "clusterCrn";

    private static final String CLUSTER_TYPE_DEFAULT = "datahub";

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String name;

    private final String type;

    private final String crn;

    private final String owner;

    private final String platform;

    private final String version;

    private FluentClusterDetails(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.crn = builder.crn;
        this.owner = builder.owner;
        this.platform = builder.platform;
        this.version = builder.version;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCrn() {
        return crn;
    }

    public String getOwner() {
        return owner;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("platform", ObjectUtils.defaultIfNull(this.platform, EMPTY_CONFIG_DEFAULT));
        map.put("clusterName", this.name);
        map.put("clusterType", ObjectUtils.defaultIfNull(this.type, CLUSTER_TYPE_DEFAULT));
        map.put(CLUSTER_CRN_KEY, this.crn);
        map.put("clusterOwner", this.owner);
        map.put("clusterVersion", this.version);
        return map;
    }

    public static final class Builder {

        private String name;

        private String type;

        private String crn;

        private String owner;

        private String platform;

        private String version;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public FluentClusterDetails build() {
            return new FluentClusterDetails(this);
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

    }
}
