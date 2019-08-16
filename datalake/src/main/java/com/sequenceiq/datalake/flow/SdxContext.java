package com.sequenceiq.datalake.flow;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SdxContext extends CommonContext {

    private Long sdxId;

    private String userId;

    private String requestId;

    private String sdxCrn;

    public SdxContext(FlowParameters flowParameters, Long sdxId, String userId, String requestId, String sdxCrn) {
        super(flowParameters);
        this.sdxId = sdxId;
        this.userId = userId;
        this.requestId = requestId;
        this.sdxCrn = sdxCrn;
    }

    public SdxContext(FlowParameters flowParameters, SdxEvent event) {
        super(flowParameters);
        sdxId = event.getResourceId();
        userId = event.getUserId();
        requestId = event.getRequestId();
        sdxCrn = event.getSdxCrn();
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSdxCrn() {
        return sdxCrn;
    }

    public void setSdxCrn(String sdxCrn) {
        this.sdxCrn = sdxCrn;
    }
}
