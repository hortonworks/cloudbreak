package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LimitsConfigurationResponse {

    private Integer maxNodeCountLimit;

    public LimitsConfigurationResponse() {
    }

    public LimitsConfigurationResponse(Integer maxNodeCountLimit) {
        this.maxNodeCountLimit = maxNodeCountLimit;
    }

    public Integer getMaxNodeCountLimit() {
        return maxNodeCountLimit;
    }

    public void setMaxNodeCountLimit(Integer maxNodeCountLimit) {
        this.maxNodeCountLimit = maxNodeCountLimit;
    }
}
