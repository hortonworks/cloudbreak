package com.sequenceiq.datalake.flow;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SdxContext extends CommonContext {

    private Long sdxId;

    public SdxContext(FlowParameters flowParameters, Long sdxId) {
        super(flowParameters);
        this.sdxId = sdxId;
    }

    public Long getSdxId() {
        return sdxId;
    }

    public void setSdxId(Long sdxId) {
        this.sdxId = sdxId;
    }
}
