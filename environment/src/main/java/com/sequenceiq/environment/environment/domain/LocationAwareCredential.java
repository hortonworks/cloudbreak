package com.sequenceiq.environment.environment.domain;

import com.sequenceiq.environment.credential.domain.Credential;

public class LocationAwareCredential {

    private final Credential credential;

    private final String location;

    private LocationAwareCredential(Builder builder) {
        this.credential = builder.credential;
        this.location = builder.location;
    }

    public Credential getCredential() {
        return credential;
    }

    public String getLocation() {
        return location;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Credential credential;

        private String location;

        public Builder withCredential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withLocation(String location) {
            this.location = location;
            return this;
        }

        public LocationAwareCredential build() {
            return new LocationAwareCredential(this);
        }
    }
}
