package com.sequenceiq.flow.core.listener;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;

public class FlowTransitionContext<S extends FlowState, E extends FlowEvent> {

    private final AbstractFlowConfiguration.FlowEdgeConfig<S, E> edgeConfig;

    private final String flowChainType;

    private final String rootFlowChainType;

    private final String actualFlowChainType;

    private final String flowType;

    private final Class<? extends Enum> stateType;

    private final Long resourceId;

    private final String flowId;

    private final String flowChainId;

    private final long startTimeInMillis;

    public FlowTransitionContext(FlowEdgeConfig<S, E> edgeConfig, String flowChainType, String flowType, Class<? extends Enum> stateType, Long resourceId,
            String flowId, String flowChainId, long startTimeInMillis) {
        this.edgeConfig = edgeConfig;
        this.flowChainType = flowChainType;
        this.rootFlowChainType = getRootFlowChainType(flowChainType);
        this.actualFlowChainType = getActualFlowChainType(flowChainType);
        this.flowType = flowType;
        this.stateType = stateType;
        this.resourceId = resourceId;
        this.flowId = flowId;
        this.flowChainId = flowChainId;
        this.startTimeInMillis = startTimeInMillis;
    }

    private String getRootFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? StringUtils.substringBefore(flowChainTypes, "/") : null;
    }

    private String getActualFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? flowChainTypes.substring(flowChainTypes.lastIndexOf('/') + 1) : null;
    }

    public FlowEdgeConfig<S, E> getEdgeConfig() {
        return edgeConfig;
    }

    public String getFlowChainType() {
        return flowChainType;
    }

    public String getRootFlowChainType() {
        return rootFlowChainType;
    }

    public String getActualFlowChainType() {
        return actualFlowChainType;
    }

    public String getFlowType() {
        return flowType;
    }

    public Class<? extends Enum> getStateType() {
        return stateType;
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

    public long getStartTimeInMillis() {
        return startTimeInMillis;
    }

    @Override
    public String toString() {
        return "FlowTransitionContext{" +
                "edgeConfig=" + edgeConfig +
                ", rootFlowChainType='" + rootFlowChainType + '\'' +
                ", actualFlowChainType='" + actualFlowChainType + '\'' +
                ", flowType='" + flowType + '\'' +
                ", stateType=" + stateType +
                ", resourceId=" + resourceId +
                ", flowId='" + flowId + '\'' +
                ", flowChainId='" + flowChainId + '\'' +
                ", startTimeInMillis='" + startTimeInMillis + '\'' +
                '}';
    }
}
