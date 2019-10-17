package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryableFlowResponse implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.OPERATION_NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.FAIL_DATE)
    private Long failDate;

    private RetryableFlowResponse(String name, Long failDate) {
        this.name = name;
        this.failDate = failDate;
    }

    public String getName() {
        return name;
    }

    public Long getFailDate() {
        return failDate;
    }

    public static class Builder {

        private String name;

        private Long failDate;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setFailDate(Long failDate) {
            this.failDate = failDate;
            return this;
        }

        public RetryableFlowResponse build() {
            return new RetryableFlowResponse(name, failDate);
        }
    }
}
