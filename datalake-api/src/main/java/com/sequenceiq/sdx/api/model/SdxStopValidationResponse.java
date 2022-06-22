package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxStopValidationResponse {
    @ApiModelProperty(ModelDescriptions.STOPPABLE)
    private boolean stoppable;

    @ApiModelProperty(ModelDescriptions.UNSTOPPABLE_REASON)
    private String unstoppableReason;

    public SdxStopValidationResponse() {
    }

    public SdxStopValidationResponse(boolean stoppable, String unstoppableReason) {
        this.stoppable = stoppable;
        this.unstoppableReason = unstoppableReason;
    }

    public boolean isStoppable() {
        return stoppable;
    }

    public void setStoppable(boolean stoppable) {
        this.stoppable = stoppable;
    }

    public String getUnstoppableReason() {
        return unstoppableReason;
    }

    public void setUnstoppableReason(String unstoppableReason) {
        this.unstoppableReason = unstoppableReason;
    }

    @Override
    public String toString() {
        return "SdxStopValidationResponse{" +
                (stoppable ? ("stoppable=true, unstoppableReason='" + unstoppableReason + '\'') : "stoppable=false") +
                '}';
    }
}
