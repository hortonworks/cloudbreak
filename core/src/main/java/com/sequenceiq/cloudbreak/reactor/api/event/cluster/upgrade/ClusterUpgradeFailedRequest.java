package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedRequest extends StackEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    private final Set<Image> candidateImages;

    @JsonCreator
    public ClusterUpgradeFailedRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStackStatus,
            @JsonProperty("candidateImages") Set<Image> candidateImages) {
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
