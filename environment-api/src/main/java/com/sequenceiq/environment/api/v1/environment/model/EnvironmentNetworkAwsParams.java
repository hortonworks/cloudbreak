package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkAwsV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkAwsParams {
    @ApiModelProperty(value = EnvironmentModelDescription.AWS_VPC_ID, required = true)
    private String vpcId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public static final class EnvironmentNetworkAwsParamsBuilder {
        private String vpcId;

        private EnvironmentNetworkAwsParamsBuilder() {
        }

        public static EnvironmentNetworkAwsParamsBuilder anEnvironmentNetworkAwsParams() {
            return new EnvironmentNetworkAwsParamsBuilder();
        }

        public EnvironmentNetworkAwsParamsBuilder withVpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public EnvironmentNetworkAwsParams build() {
            EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
            environmentNetworkAwsParams.setVpcId(vpcId);
            return environmentNetworkAwsParams;
        }
    }
}
