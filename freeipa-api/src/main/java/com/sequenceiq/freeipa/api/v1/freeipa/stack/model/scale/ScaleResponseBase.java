package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;

public abstract class ScaleResponseBase {

    private FlowIdentifier flowIdentifier;

    private AvailabilityType originalAvailabilityType;

    private AvailabilityType targetAvailabilityType;

    private String operationId;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public AvailabilityType getOriginalAvailabilityType() {
        return originalAvailabilityType;
    }

    public void setOriginalAvailabilityType(AvailabilityType originalAvailabilityType) {
        this.originalAvailabilityType = originalAvailabilityType;
    }

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    public void setTargetAvailabilityType(AvailabilityType targetAvailabilityType) {
        this.targetAvailabilityType = targetAvailabilityType;
    }

    @Override
    public String toString() {
        return "ScaleResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", originalAvailabilityType=" + originalAvailabilityType +
                ", targetAvailabilityType=" + targetAvailabilityType +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
