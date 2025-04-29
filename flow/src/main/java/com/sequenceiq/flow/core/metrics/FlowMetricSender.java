package com.sequenceiq.flow.core.metrics;

import static com.sequenceiq.flow.core.FlowMetricTag.ACTUAL_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricTag.FLOW;
import static com.sequenceiq.flow.core.FlowMetricTag.ROOT_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FAILED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FINISHED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_STARTED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_TIME;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;

@Component
public class FlowMetricSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowMetricSender.class);

    private static final String NONE = "NONE";

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    public void send(FlowTransitionContext flowTransitionContext, String nextFlowState, String flowEvent) {
        try {
            String rootFlowChainType = flowTransitionContext.getRootFlowChainType() != null ? flowTransitionContext.getRootFlowChainType() : NONE;
            String actualFlowChainType = flowTransitionContext.getActualFlowChainType() != null ? flowTransitionContext.getActualFlowChainType() : NONE;
            String flowType = flowTransitionContext.getFlowType();
            AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowTransitionContext.getEdgeConfig();
            Enum nextFlowStateEnum = FlowStateUtil.getFlowStateEnum(flowTransitionContext.getStateType(), nextFlowState, flowEvent);
            if (nextFlowStateEnum == null) {
                LOGGER.debug("nextFlowStateEnum is null for flow type: '{}', flow chain type: '{}', next flow state: '{}', flow event: '{}'," +
                                " flow metrics is not recorded!", flowType, flowTransitionContext.getFlowChainType(), nextFlowState, flowEvent);
            } else if (edgeConfig.getInitState().equals(nextFlowStateEnum)) {
                metricService.incrementMetricCounter(FLOW_STARTED,
                        ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                        ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                        FLOW.name(), flowType);
            } else if (edgeConfig.getFinalState().equals(nextFlowStateEnum)) {
                long duration = System.currentTimeMillis() - flowTransitionContext.getStartTimeInMillis();
                if (flowEvent != null) {
                    metricService.recordTimer(duration,
                            FLOW_TIME,
                            ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                            ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                            FLOW.name(), flowType);
                }
                metricService.incrementMetricCounter(FLOW_FINISHED,
                        ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                        ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                        FLOW.name(), flowType);
            } else if (edgeConfig.getDefaultFailureState().equals(nextFlowStateEnum)) {
                metricService.incrementMetricCounter(FLOW_FAILED,
                        ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                        ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                        FLOW.name(), flowType);
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot record flow metrics!", e);
        }
    }
}
