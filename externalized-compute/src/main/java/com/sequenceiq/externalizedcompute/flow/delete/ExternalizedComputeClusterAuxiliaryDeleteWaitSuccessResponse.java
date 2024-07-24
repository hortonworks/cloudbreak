package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse extends ExternalizedComputeClusterDeleteEvent {

    @JsonCreator
    public ExternalizedComputeClusterAuxiliaryDeleteWaitSuccessResponse(
            @JsonProperty("resourceId") Long externalizedComputeClusterId,
            @JsonProperty("actorCrn") String actorCrn,
            @JsonProperty("force") boolean force,
            @JsonProperty("preserveCluster") boolean preserveCluster) {
        super(externalizedComputeClusterId, actorCrn, force, preserveCluster);
    }

    @Override
    public String selector() {
        return "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT";
    }

}
