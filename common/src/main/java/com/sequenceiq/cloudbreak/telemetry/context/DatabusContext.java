package com.sequenceiq.cloudbreak.telemetry.context;

import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

public class DatabusContext {

    private final boolean enabled;

    private final String endpoint;

    private final String s3Endpoint;

    private final DataBusCredential credential;

    private final boolean validation;

    private DatabusContext(Builder builder) {
        this.enabled = builder.enabled;
        this.endpoint = builder.endpoint;
        this.s3Endpoint = builder.s3Endpoint;
        this.credential = builder.credential;
        this.validation = builder.validation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public DataBusCredential getCredential() {
        return credential;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public boolean isValidation() {
        return validation;
    }

    @Override
    public String toString() {
        return "DatabusContext{" +
                "enabled=" + enabled +
                ", endpoint='" + endpoint + '\'' +
                ", s3Endpoint='" + s3Endpoint + '\'' +
                ", validation=" + validation +
                '}';
    }

    public static class Builder {

        private boolean enabled;

        private String endpoint;

        private String s3Endpoint;

        private DataBusCredential credential;

        private boolean validation;

        private Builder() {
        }

        public DatabusContext build() {
            return new DatabusContext(this);
        }

        public Builder enabled() {
            this.enabled = true;
            return this;
        }

        public Builder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder withCredential(DataBusCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withValidation() {
            this.validation = true;
            return this;
        }

        public Builder withS3Endpoint(String s3Endpoint) {
            this.s3Endpoint = s3Endpoint;
            return this;
        }

    }
}
