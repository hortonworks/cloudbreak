package com.sequenceiq.cloudbreak.service.externaldatabase.model;

import java.util.Map;

public class DatabaseServerParameter {

    private final boolean highlyAvailable;

    private final String engineVersion;

    private final Map<String, Object> attributes;

    private DatabaseServerParameter(Builder builder) {
        this.highlyAvailable = builder.highlyAvailable;
        this.engineVersion = builder.engineVersion;
        this.attributes = builder.attributes != null ? builder.attributes : Map.of();
    }

    public boolean isHighlyAvailable() {
        return highlyAvailable;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean highlyAvailable;

        private String engineVersion;

        private Map<String, Object> attributes;

        public Builder withHighlyAvailable(boolean highlyAvailable) {
            this.highlyAvailable = highlyAvailable;
            return this;
        }

        public Builder withEngineVersion(String engineVersion) {
            this.engineVersion = engineVersion;
            return this;
        }

        public Builder withAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public DatabaseServerParameter build() {
            return new DatabaseServerParameter(this);
        }
    }
}
