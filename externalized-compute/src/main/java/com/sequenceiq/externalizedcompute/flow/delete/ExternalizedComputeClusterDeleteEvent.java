package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;

public class ExternalizedComputeClusterDeleteEvent extends ExternalizedComputeClusterEvent {

    private final boolean force;

    private final boolean preserveCluster;

    public ExternalizedComputeClusterDeleteEvent(String selector, Long externalizedComputeId, String userId, boolean force, boolean preserveCluster) {
        super(selector, externalizedComputeId, userId);
        this.force = force;
        this.preserveCluster = preserveCluster;
    }

    public ExternalizedComputeClusterDeleteEvent(String selector, Long externalizedComputeId, String userId, boolean force, boolean preserveCluster,
            Promise<AcceptResult> accepted) {
        super(selector, externalizedComputeId, userId, accepted);
        this.force = force;
        this.preserveCluster = preserveCluster;
    }

    public ExternalizedComputeClusterDeleteEvent(Long externalizedComputeId, String actorCrn, boolean force, boolean preserveCluster) {
        super(externalizedComputeId, actorCrn);
        this.force = force;
        this.preserveCluster = preserveCluster;
    }

    @JsonCreator
    public ExternalizedComputeClusterDeleteEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long externalizedComputeId,
            @JsonProperty("externalizedComputeName") String externalizedComputeName,
            @JsonProperty("userId") String userId,
            @JsonProperty("force") boolean force,
            @JsonProperty("preserveCluster") boolean preserveCluster,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, externalizedComputeId, externalizedComputeName, userId, accepted);
        this.force = force;
        this.preserveCluster = preserveCluster;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isPreserveCluster() {
        return preserveCluster;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterDeleteEvent{" +
                "force=" + force +
                ", preserveCluster=" + preserveCluster +
                "} " + super.toString();
    }
}
