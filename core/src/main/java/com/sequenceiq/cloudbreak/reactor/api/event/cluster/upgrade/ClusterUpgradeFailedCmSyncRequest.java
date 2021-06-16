package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedCmSyncRequest extends StackEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    private final Set<Image> candidateImages;

    public ClusterUpgradeFailedCmSyncRequest(Long stackId, Exception exception, DetailedStackStatus detailedStackStatus, Set<Image> candidateImages) {
        super(stackId);
        this.exception = exception;
        this.detailedStatus = detailedStackStatus;
        this.candidateImages = candidateImages;
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }

    public Set<Image> getCandidateImages() {
        return candidateImages;
    }
}
