package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsDeletionWaitRequest extends SdxEvent {

    public RdsDeletionWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }
}
