package com.sequenceiq.datalake.flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SdxContext extends CommonContext {

    private Long sdxId;

    private String userId;

    @JsonCreator
    public SdxContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("sdxId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(flowParameters);
        this.sdxId = sdxId;
        this.userId = userId;
    }

    public SdxContext(FlowParameters flowParameters, SdxEvent event) {
        super(flowParameters);
        sdxId = event.getResourceId();
        userId = event.getUserId();
    }

    public static SdxContext from(FlowParameters flowParameters, SdxEvent event) {
        return new SdxContext(flowParameters, event);
    }

    public Long getSdxId() {
        return sdxId;
    }

    public void setSdxId(Long sdxId) {
        this.sdxId = sdxId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
