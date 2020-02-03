package com.sequenceiq.cloudbreak.service.externaldatabase.model;

public class DatabaseServerParameter {

    private final boolean highlyAvailable;

    public DatabaseServerParameter(Builder builder) {
        this.highlyAvailable = builder.highlyAvailable;
    }

    public boolean isHighlyAvailable() {
        return highlyAvailable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean highlyAvailable;

        public Builder withHighlyAvailable(boolean highlyAvailable) {
            this.highlyAvailable = highlyAvailable;
            return this;
        }

        public DatabaseServerParameter build() {
            return new DatabaseServerParameter(this);
        }
    }
}
