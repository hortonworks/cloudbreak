package com.sequenceiq.flow.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryableFlowResponse implements JsonEntity {

    @ApiModelProperty("Name of the failed operation, that is also retryable.")
    private String name;

    @ApiModelProperty("Date when the operation went failed.")
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

    @Override
    public String toString() {
        return "RetryableFlowResponse{" +
                "name='" + name + '\'' +
                ", failDate=" + failDate +
                '}';
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
