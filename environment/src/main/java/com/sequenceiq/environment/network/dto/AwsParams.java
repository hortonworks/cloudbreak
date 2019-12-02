package com.sequenceiq.environment.network.dto;

public class AwsParams {

    private String vpcId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public String toString() {
        return "AwsParams{" +
                "vpcId='" + vpcId + '\'' +
                '}';
    }

    public static final class AwsParamsBuilder {
        private String vpcId;

        private AwsParamsBuilder() {
        }

        public static AwsParamsBuilder anAwsParams() {
            return new AwsParamsBuilder();
        }

        public AwsParamsBuilder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public AwsParams build() {
            AwsParams awsParams = new AwsParams();
            awsParams.setVpcId(vpcId);
            return awsParams;
        }
    }
}
