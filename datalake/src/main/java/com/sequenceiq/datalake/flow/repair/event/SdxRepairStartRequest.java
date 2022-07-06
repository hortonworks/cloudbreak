package com.sequenceiq.datalake.flow.repair.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.settings.SdxRepairSettings;

public class SdxRepairStartRequest extends SdxEvent {

    private final SdxRepairSettings repairSettings;

    @JsonCreator
    public SdxRepairStartRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("repairSettings") SdxRepairSettings repairSettings) {
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
