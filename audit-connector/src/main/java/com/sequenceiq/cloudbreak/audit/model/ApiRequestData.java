package com.sequenceiq.cloudbreak.audit.model;

public class ApiRequestData extends EventData {

    private final String requestParameters;

    private final boolean mutating;

    private final String apiVersion;

    private final String userAgent;

    public ApiRequestData(Builder builder) {
        this.requestParameters = builder.requestParameters;
        this.mutating = builder.mutating;
        this.apiVersion = builder.apiVersion;
        this.userAgent = builder.userAgent;
    }

    public String getRequestParameters() {
        return requestParameters;
    }

    public boolean isMutating() {
        return mutating;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ApiRequestData{" +
                "requestParameters='" + requestParameters + '\'' +
                ", mutating=" + mutating +
                ", apiVersion='" + apiVersion + '\'' +
                ", userAgent='" + userAgent + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private String requestParameters;

        private boolean mutating;

        private String apiVersion;

        private String userAgent;

        public Builder withRequestParameters(String requestParameters) {
            this.requestParameters = requestParameters;
            return this;
        }

        public Builder withMutating(boolean mutating) {
            this.mutating = mutating;
            return this;
        }

        public Builder withApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public ApiRequestData build() {
            return new ApiRequestData(this);
        }
    }
}
