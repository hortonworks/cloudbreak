package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.settings.SdxRepairSettings;

public class SdxRepairStartRequest extends SdxEvent {

    private final SdxRepairSettings repairSettings;

    public SdxRepairStartRequest(Long sdxId, String userId, SdxRepairSettings repairSettings) {
        super(sdxId, userId);
        this.repairSettings = repairSettings;
    }

    public SdxRepairSettings getRepairSettings() {
        return repairSettings;
    }

    @Override
    public String selector() {
        return "SdxRepairStartRequest";
    }
}
