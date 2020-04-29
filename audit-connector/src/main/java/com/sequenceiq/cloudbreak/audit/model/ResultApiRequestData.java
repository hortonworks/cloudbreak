package com.sequenceiq.cloudbreak.audit.model;

public class ResultApiRequestData extends ResultEventData {

    private final String responseParameters;

    public ResultApiRequestData(Builder builder) {
        this.responseParameters = builder.responseParameters;
    }

    public String getResponseParameters() {
        return responseParameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ResultApiRequestData{" +
                "responseParameters='" + responseParameters + '\'' +
                "} " + super.toString();
    }

    public static class Builder {

        private String responseParameters;

        public Builder withResponseParameters(String responseParameters) {
            this.responseParameters = responseParameters;
            return this;
        }

        public ResultApiRequestData build() {
            return new ResultApiRequestData(this);
        }
    }
}
