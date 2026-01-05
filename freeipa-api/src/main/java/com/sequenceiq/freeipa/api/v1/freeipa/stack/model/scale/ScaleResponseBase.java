package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.BaseFlowIdentifierResponse;

public abstract class ScaleResponseBase extends BaseFlowIdentifierResponse {

    private AvailabilityType originalAvailabilityType;

    private AvailabilityType targetAvailabilityType;

    private String operationId;

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
                super.toString() +
                ", originalAvailabilityType=" + originalAvailabilityType +
                ", targetAvailabilityType=" + targetAvailabilityType +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
