package com.sequenceiq.externalizedcompute.flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ExternalizedComputeClusterContext extends CommonContext {

    private Long externalizedComputeId;

    private String userId;

    @JsonCreator
    public ExternalizedComputeClusterContext(
            @JsonProperty("flowParameters") FlowParameters flowParameters,
            @JsonProperty("externalizedComputeId") Long externalizedComputeId,
            @JsonProperty("userId") String userId) {
        super(flowParameters);
        this.externalizedComputeId = externalizedComputeId;
        this.userId = userId;
    }

    public ExternalizedComputeClusterContext(FlowParameters flowParameters, ExternalizedComputeClusterEvent event) {
        super(flowParameters);
        externalizedComputeId = event.getResourceId();
        userId = event.getUserId();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterContext{" +
                "externalizedComputeId=" + externalizedComputeId +
                ", userId='" + userId + '\'' +
                "} " + super.toString();
    }
}
