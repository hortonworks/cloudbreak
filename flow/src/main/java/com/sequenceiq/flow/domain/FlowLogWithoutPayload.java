package com.sequenceiq.flow.domain;

import com.sequenceiq.flow.api.model.operation.OperationType;

public interface FlowLogWithoutPayload {

    Long getResourceId();

    Long getCreated();

    Long getEndTime();

    String getFlowId();

    String getFlowChainId();

    String getNextEvent();

    ClassValue getPayloadType();

    ClassValue getFlowType();

    String getVariables();

    String getCurrentState();

    Boolean getFinalized();

    String getCloudbreakNodeId();

    Long getVersion();

    StateStatus getStateStatus();

    String getResourceType();

    String getFlowTriggerUserCrn();

    OperationType getOperationType();

    String getReason();

    default String minimizedString() {
        return "FlowLog{" +
                "resourceId=" + getResourceId() +
                ", created=" + getCreated() +
                ", flowId='" + getFlowId() + '\'' +
                ", currentState='" + getCurrentState() + '\'' +
                ", stateStatus=" + getStateStatus() +
                ", nextEvent=" + getNextEvent() +
                ", operationType=" + getOperationType() +
                '}';
    }
}
