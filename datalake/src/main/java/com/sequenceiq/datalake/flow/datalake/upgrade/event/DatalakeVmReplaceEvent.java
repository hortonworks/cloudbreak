package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_VM_REPLACE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeVmReplaceEvent extends SdxEvent {

    @JsonCreator
    public DatalakeVmReplaceEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return DATALAKE_VM_REPLACE_EVENT.event();
    }

    @Override
    public String toString() {
        return "DatalakeVmReplaceEvent{} " + super.toString();
    }
}
