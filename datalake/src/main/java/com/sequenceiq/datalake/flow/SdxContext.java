package com.sequenceiq.datalake.flow;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SdxContext extends CommonContext {

    private Long sdxId;

    private String userId;

    public SdxContext(FlowParameters flowParameters, Long sdxId, String userId) {
        super(flowParameters);
        this.sdxId = sdxId;
        this.userId = userId;
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
