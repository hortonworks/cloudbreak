package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class CmSyncRequest extends ClusterPlatformRequest {

    private final Set<String> candidateImageUuids;

    private final String flowTriggerUserCrn;

    public CmSyncRequest(Long stackId, Set<String> candidateImageUuids, String flowTriggerUserCrn) {
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
}
