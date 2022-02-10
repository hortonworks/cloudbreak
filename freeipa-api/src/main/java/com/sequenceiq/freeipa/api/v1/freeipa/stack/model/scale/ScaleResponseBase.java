package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;

public abstract class ScaleResponseBase {

    private FlowIdentifier flowIdentifier;

    private FormFactor originalFormFactor;

    private FormFactor targetFormFactor;

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

    public FormFactor getOriginalFormFactor() {
        return originalFormFactor;
    }

    public void setOriginalFormFactor(FormFactor originalFormFactor) {
        this.originalFormFactor = originalFormFactor;
    }

    public FormFactor getTargetFormFactor() {
        return targetFormFactor;
    }

    public void setTargetFormFactor(FormFactor targetFormFactor) {
        this.targetFormFactor = targetFormFactor;
    }

    @Override
    public String toString() {
        return "ScaleResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", originalFormFactor=" + originalFormFactor +
                ", targetFormFactor=" + targetFormFactor +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
