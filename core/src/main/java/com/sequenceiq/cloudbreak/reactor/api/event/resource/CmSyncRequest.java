package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class CmSyncRequest extends ClusterPlatformRequest {

    private final Set<String> candidateImageUuids;

    private final String flowTriggerUserCrn;

    @JsonCreator
    public CmSyncRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("candidateImageUuids") Set<String> candidateImageUuids,
            @JsonProperty("flowTriggerUserCrn") String flowTriggerUserCrn) {
        super(stackId);
        this.candidateImageUuids = candidateImageUuids;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
    }

    public Set<String> getCandidateImageUuids() {
        return candidateImageUuids;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    @Override
    public String toString() {
        return "CmSyncRequest{" +
                "candidateImageUuids=" + candidateImageUuids +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                "} " + super.toString();
    }
}
