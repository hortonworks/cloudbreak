package com.sequenceiq.environment.network.dto;

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
