package com.sequenceiq.flow.core;

import static com.sequenceiq.flow.core.FlowConstants.FLOW_RESTARTED;

import java.util.HashMap;
import java.util.Map;

public class RestartContext {

    private final Long resourceId;

    private final String flowId;

    private final String flowChainId;

    private final String flowTriggerUserCrn;

    private final String flowOperationType;

    private final String event;

    private final Map<Object, Object> contextParams;

    private RestartContext(
            Long resourceId,
            String flowId,
            String flowChainId,
            String flowTriggerUserCrn,
            String flowOperationType,
            String event,
            Map<Object, Object> contextParams) {
        this.resourceId = resourceId;
        this.flowId = flowId;
        this.flowChainId = flowChainId;
        this.flowTriggerUserCrn = flowTriggerUserCrn;
        this.flowOperationType = flowOperationType;
        this.event = event;
        this.contextParams = new HashMap<>();
        this.contextParams.put(FLOW_RESTARTED, Boolean.TRUE);
        if (contextParams != null) {
            this.contextParams.putAll(contextParams);
        }
    }

    public static RestartContext flowRestart(
            Long resourceId,
            String flowId,
            String flowChainId,
            String flowTriggerUserCrn,
            String flowOperationType,
            String event) {
        return new RestartContext(resourceId, flowId, flowChainId, flowTriggerUserCrn, flowOperationType, event, Map.of());
    }

    public static RestartContext flowChainRestart(
            Long resourceId,
            String flowChainId,
            String flowTriggerUserCrn,
            String flowOperationType,
            Map<Object, Object> contextParams) {
        return new RestartContext(resourceId, null, flowChainId, flowTriggerUserCrn, flowOperationType, null, contextParams);
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public String getFlowTriggerUserCrn() {
        return flowTriggerUserCrn;
    }

    public String getFlowOperationType() {
        return flowOperationType;
    }

    public String getEvent() {
        return event;
    }

    public Map<Object, Object> getContextParams() {
        return contextParams;
    }

    @Override
    public String toString() {
        return "RestartContext{" +
                "resourceId=" + resourceId +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", flowTriggerUserCrn='" + flowTriggerUserCrn + '\'' +
                ", flowOperationType='" + flowOperationType + '\'' +
                ", event='" + event + '\'' +
                '}';
    }
}
