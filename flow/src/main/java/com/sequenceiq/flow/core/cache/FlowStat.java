package com.sequenceiq.flow.core.cache;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.FlowConfiguration;

public class FlowStat {

    private String flowId;

    private String flowChainId;

    private Long startTime;

    private boolean restored;

    private Class<? extends FlowConfiguration<?>> flowType;

    private Long resourceId;

    private OperationType operationType;

    private PayloadContext payloadContext;

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public boolean isRestored() {
        return restored;
    }

    public void setRestored(boolean restored) {
        this.restored = restored;
    }

    public Class<? extends FlowConfiguration<?>> getFlowType() {
        return flowType;
    }

    public void setFlowType(Class<? extends FlowConfiguration<?>> flowType) {
        this.flowType = flowType;
    }

    public PayloadContext getPayloadContext() {
        return payloadContext;
    }

    public void setPayloadContext(PayloadContext payloadContext) {
        this.payloadContext = payloadContext;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowChainId() {
        return flowChainId;
    }

    public void setFlowChainId(String flowChainId) {
        this.flowChainId = flowChainId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
}
