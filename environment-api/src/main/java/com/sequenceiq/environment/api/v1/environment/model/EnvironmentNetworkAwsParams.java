package com.sequenceiq.environment.api.v1.environment.model;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentNetworkAwsV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkAwsParams {

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.AWS_VPC_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String vpcId;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public String toString() {
        return "EnvironmentNetworkAwsParams{" +
                "vpcId='" + vpcId + '\'' +
                '}';
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
