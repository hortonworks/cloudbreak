package com.sequenceiq.datalake.flow;

import com.sequenceiq.flow.core.CommonContext;

public class SdxContext extends CommonContext {

    private Long sdxId;

    public SdxContext(String flowId, Long sdxId) {
        super(flowId);
        this.sdxId = sdxId;
    }

    public Long getSdxId() {
        return sdxId;
    }

    public void setSdxId(Long sdxId) {
        this.sdxId = sdxId;
    }
}
