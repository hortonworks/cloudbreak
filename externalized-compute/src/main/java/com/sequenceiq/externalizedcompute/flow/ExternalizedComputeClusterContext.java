package com.sequenceiq.externalizedcompute.flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ExternalizedComputeClusterContext extends CommonContext {

    private Long externalizedComputeId;

    private String actorCrn;

    @JsonCreator
    public ExternalizedComputeClusterContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("externalizedComputeId") Long externalizedComputeId,
            @JsonProperty("actorCrn") String actorCrn) {
        super(flowParameters);
        this.externalizedComputeId = externalizedComputeId;
        this.actorCrn = actorCrn;
    }

    public ExternalizedComputeClusterContext(FlowParameters flowParameters, ExternalizedComputeClusterEvent event) {
        super(flowParameters);
        externalizedComputeId = event.getResourceId();
        actorCrn = event.getActorCrn();
    }

    public static ExternalizedComputeClusterContext from(FlowParameters flowParameters, ExternalizedComputeClusterEvent event) {
        return new ExternalizedComputeClusterContext(flowParameters, event);
    }

    public Long getExternalizedComputeId() {
        return externalizedComputeId;
    }

    public void setExternalizedComputeId(Long externalizedComputeId) {
        this.externalizedComputeId = externalizedComputeId;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public void setActorCrn(String actorCrn) {
        this.actorCrn = actorCrn;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterContext{" +
                "externalizedComputeId=" + externalizedComputeId +
                ", actorCrn='" + actorCrn + '\'' +
                "} " + super.toString();
    }
}
