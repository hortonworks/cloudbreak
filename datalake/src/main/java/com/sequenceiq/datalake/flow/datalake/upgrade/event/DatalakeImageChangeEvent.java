package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeImageChangeEvent extends SdxEvent {

    private final String imageId;

    public DatalakeImageChangeEvent(Long sdxId, String userId, String imageId) {
        super(sdxId, userId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String selector() {
        return "DatalakeImageChangeEvent";
    }
}
