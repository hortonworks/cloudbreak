package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CmSyncTriggerEvent extends StackEvent {

    private final Set<String> candidateImageUuids;

    public CmSyncTriggerEvent(Long stackId, Set<String> candidateImageUuids) {
        super(stackId);
        this.candidateImageUuids = candidateImageUuids;
    }

    public Set<String> getCandidateImageUuids() {
        return candidateImageUuids;
    }

    @Override
    public String toString() {
        return "CmSyncTriggerEvent{" +
                "candidateImageUuids=" + candidateImageUuids +
                "} " + super.toString();
    }
}
