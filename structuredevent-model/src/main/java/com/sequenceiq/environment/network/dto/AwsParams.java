package com.sequenceiq.environment.network.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AwsParams.Builder.class)
public class AwsParams {

    private final String vpcId;

    private AwsParams(Builder builder) {
        vpcId = builder.vpcId;
    }

    public String getVpcId() {
        return vpcId;
    }

    @Override
    public String toString() {
        return "AwsParams{" +
                "vpcId='" + vpcId + '\'' +
                '}';
    }

    public static AwsParams.Builder builder() {
        return new AwsParams.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String vpcId;

        private Builder() {
        }

        public Builder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public AwsParams build() {
            return new AwsParams(this);
        }
    }
}
