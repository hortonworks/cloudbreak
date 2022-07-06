package com.sequenceiq.datalake.flow.repair.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.settings.SdxRepairSettings;

public class SdxRepairStartEvent extends SdxEvent {

    private SdxRepairSettings repairSettings;

    @JsonCreator
    public SdxRepairStartEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("repairSettings") SdxRepairSettings repairSettings) {
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

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxRepairStartEvent.class, other,
                event -> Objects.equals(repairSettings, event.repairSettings));
    }
}
