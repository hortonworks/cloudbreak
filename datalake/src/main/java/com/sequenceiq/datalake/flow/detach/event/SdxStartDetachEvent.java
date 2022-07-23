package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class SdxStartDetachEvent extends SdxEvent {

    private final SdxCluster sdxCluster;

    private boolean detachDuringRecovery;

    public SdxStartDetachEvent(String selector, Long sdxId, SdxCluster newSdxCluster, String userId) {
        super(selector, sdxId, userId);
        sdxCluster = newSdxCluster;
    }

    @JsonCreator
    public SdxStartDetachEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxCluster") SdxCluster sdxCluster,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.sdxCluster = sdxCluster;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    public void setDetachDuringRecovery(boolean detachDuringRecovery) {
        this.detachDuringRecovery = detachDuringRecovery;
    }

    public boolean isDetachDuringRecovery() {
        return detachDuringRecovery;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxStartDetachEvent.class, other,
                event -> Objects.equals(sdxCluster, event.sdxCluster));
    }
}
