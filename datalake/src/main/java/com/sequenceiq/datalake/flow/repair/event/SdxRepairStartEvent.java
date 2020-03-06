package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.settings.SdxRepairSettings;

public class SdxRepairStartEvent extends SdxEvent {

    private SdxRepairSettings repairSettings;

    public SdxRepairStartEvent(String selector, Long sdxId, String userId, SdxRepairSettings repairSettings) {
        super(selector, sdxId, userId);
        this.repairSettings = repairSettings;
    }

    @Override
    public String selector() {
        return "SdxRepairStartEvent";
    }

    public SdxRepairSettings getRepairSettings() {
        return repairSettings;
    }

    public void setRepairSettings(SdxRepairSettings repairSettings) {
        this.repairSettings = repairSettings;
    }
}
