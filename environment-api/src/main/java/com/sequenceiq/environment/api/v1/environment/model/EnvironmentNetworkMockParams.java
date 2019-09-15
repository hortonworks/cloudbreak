package com.sequenceiq.environment.api.v1.environment.model;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkMockV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentNetworkMockParams {

    @Size(max = 255)
    @ApiModelProperty(value = EnvironmentModelDescription.AWS_VPC_ID, required = true)
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

    public static final class EnvironmentNetworkMockParamsBuilder {

        private String vpcId;

        private String internetGatewayId;

        private EnvironmentNetworkMockParamsBuilder() {
        }

        public static EnvironmentNetworkMockParamsBuilder anEnvironmentNetworkMockParams() {
            return new EnvironmentNetworkMockParamsBuilder();
        }

        public EnvironmentNetworkMockParamsBuilder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public EnvironmentNetworkMockParamsBuilder withInternetGatewayId(String gatewayId) {
            internetGatewayId = gatewayId;
            return this;
        }

        public EnvironmentNetworkMockParams build() {
            EnvironmentNetworkMockParams environmentNetworkMockParams = new EnvironmentNetworkMockParams();
            environmentNetworkMockParams.setVpcId(vpcId);
            environmentNetworkMockParams.setInternetGatewayId(internetGatewayId);
            return environmentNetworkMockParams;
        }
    }

}
