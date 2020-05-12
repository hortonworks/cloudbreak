package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeVmReplaceEvent extends SdxEvent {

    public DatalakeVmReplaceEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "DatalakeVmReplaceEvent";
    }
}
