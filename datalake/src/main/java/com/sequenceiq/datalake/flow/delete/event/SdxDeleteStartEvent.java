package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxDeleteStartEvent extends SdxEvent {

    private final boolean forced;

    public SdxDeleteStartEvent(String selector, Long sdxId, String userId, boolean forced) {
        super(selector, sdxId, userId);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }
}
