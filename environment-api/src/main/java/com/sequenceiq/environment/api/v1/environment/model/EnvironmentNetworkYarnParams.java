package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkYarnV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentNetworkYarnParams {
    @ApiModelProperty(value = EnvironmentModelDescription.AWS_VPC_ID, required = true)
    private String queue;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public static final class EnvironmentNetworkYarnParamsBuilder {
        private String queue;

        private EnvironmentNetworkYarnParamsBuilder() {
        }

        public static EnvironmentNetworkYarnParamsBuilder anEnvironmentNetworkYarnParams() {
            return new EnvironmentNetworkYarnParamsBuilder();
        }

        public EnvironmentNetworkYarnParamsBuilder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public EnvironmentNetworkYarnParams build() {
            EnvironmentNetworkYarnParams environmentNetworkYarnParams = new EnvironmentNetworkYarnParams();
            environmentNetworkYarnParams.setQueue(queue);
            return environmentNetworkYarnParams;
        }
    }
}
