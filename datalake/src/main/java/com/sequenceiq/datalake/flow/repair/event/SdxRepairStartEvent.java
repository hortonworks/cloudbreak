package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

public class SdxRepairStartEvent extends SdxEvent {

    private SdxRepairRequest repairRequest;

    public SdxRepairStartEvent(String selector, Long sdxId, String userId, SdxRepairRequest repairRequest) {
        super(selector, sdxId, userId);
        this.repairRequest = repairRequest;
    }

    @Override
    public String selector() {
        return "SdxRepairStartEvent";
    }

    public SdxRepairRequest getRepairRequest() {
        return repairRequest;
    }

    public void setRepairRequest(SdxRepairRequest repairRequest) {
        this.repairRequest = repairRequest;
    }
}
