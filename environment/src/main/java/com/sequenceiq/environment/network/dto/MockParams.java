package com.sequenceiq.environment.network.dto;

public class MockParams {

    private String vpcId;

    private String internetGatewayId;

    public String getInternetGatewayId() {
        return internetGatewayId;
    }

    public void setInternetGatewayId(String internetGatewayId) {
        this.internetGatewayId = internetGatewayId;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public static final class MockParamsBuilder {

        private String vpcId;

        private String internetGatewayId;

        private MockParamsBuilder() {
        }

        public static MockParamsBuilder aMockParams() {
            return new MockParamsBuilder();
        }

        public MockParamsBuilder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public MockParamsBuilder withInternetGatewayId(String gatewayId) {
            internetGatewayId = gatewayId;
            return this;
        }

        public MockParams build() {
            MockParams mockParams = new MockParams();
            mockParams.setVpcId(vpcId);
            mockParams.setInternetGatewayId(internetGatewayId);
            return mockParams;
        }

    }

}
