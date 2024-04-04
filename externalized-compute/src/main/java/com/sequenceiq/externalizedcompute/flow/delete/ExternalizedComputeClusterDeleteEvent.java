package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;

public class ExternalizedComputeClusterDeleteEvent extends ExternalizedComputeClusterEvent {

    private final boolean force;

    public ExternalizedComputeClusterDeleteEvent(String selector, Long externalizedComputeId, String userId, boolean force) {
        super(selector, externalizedComputeId, userId);
        this.force = force;
    }

    public ExternalizedComputeClusterDeleteEvent(Long externalizedComputeId, String actorCrn, boolean force) {
        super(externalizedComputeId, actorCrn);
        this.force = force;
    }

    @JsonCreator
    public ExternalizedComputeClusterDeleteEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long externalizedComputeId,
            @JsonProperty("externalizedComputeName") String externalizedComputeName,
            @JsonProperty("userId") String userId,
            @JsonProperty("force") boolean force) {
        super(selector, externalizedComputeId, externalizedComputeName, userId);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterDeleteEvent{" +
                "force=" + force +
                "} " + super.toString();
    }
}
