package com.sequenceiq.environment.network.dto;

public class MockParams {

    private final String vpcId;

    private final String internetGatewayId;

    private MockParams(Builder builder) {
        vpcId = builder.vpcId;
        internetGatewayId = builder.internetGatewayId;
    }

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public String getVpcId() {
        return vpcId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "MockParams{" +
                "vpcId='" + vpcId + '\'' +
                ", internetGatewayId='" + internetGatewayId + '\'' +
                '}';
    }

    public static final class Builder {

        private String vpcId;

        private String internetGatewayId;

        private Builder() {
        }

        public Builder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public Builder withInternetGatewayId(String gatewayId) {
            internetGatewayId = gatewayId;
            return this;
        }

        public MockParams build() {
            return new MockParams(this);
        }

    }
}
