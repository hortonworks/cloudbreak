package com.sequenceiq.datalake.flow.delete.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDeletionFailedEvent extends SdxFailedEvent {

    private final boolean forced;

    public SdxDeletionFailedEvent(Long sdxId, String userId, Exception exception, boolean forced) {
        super(sdxId, userId, exception);
        this.forced = forced;
    }

    public static SdxDeletionFailedEvent from(SdxEvent event, Exception exception, boolean forced) {
        return new SdxDeletionFailedEvent(event.getResourceId(), event.getUserId(), exception, forced);
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String selector() {
        return "SdxDeletionFailedEvent";
    }
}
