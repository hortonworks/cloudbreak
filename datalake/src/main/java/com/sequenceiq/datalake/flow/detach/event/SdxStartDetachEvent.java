package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.detach.SdxDetachEvent;

public class SdxStartDetachEvent extends SdxEvent {

    private final SdxCluster sdxCluster;

    public SdxStartDetachEvent(String selector, Long sdxId, SdxCluster newSdxCluster, String userId) {
        super(selector, sdxId, userId);
        sdxCluster = newSdxCluster;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    @Override
    public String selector() {
        return SdxDetachEvent.SDX_DETACH_EVENT.selector();
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartDetachEvent.class, other,
                event -> Objects.equals(sdxCluster, event.sdxCluster));
    }
}
