package com.sequenceiq.cloudbreak.service.externaldatabase.model;

public class DatabaseServerParameter {

    private final boolean highlyAvailable;

    private final String engineVersion;

    private DatabaseServerParameter(Builder builder) {
        this.highlyAvailable = builder.highlyAvailable;
        this.engineVersion = builder.engineVersion;
    }

    public boolean isHighlyAvailable() {
        return highlyAvailable;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean highlyAvailable;

        private String engineVersion;

        public Builder withHighlyAvailable(boolean highlyAvailable) {
            this.highlyAvailable = highlyAvailable;
            return this;
        }

        public Builder withEngineVersion(String engineVersion) {
            this.engineVersion = engineVersion;
            return this;
        }

        public DatabaseServerParameter build() {
            return new DatabaseServerParameter(this);
        }
    }
}
