package com.sequenceiq.flow.core.metrics;

import static com.sequenceiq.flow.core.FlowMetricTag.ACTUAL_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricTag.FLOW;
import static com.sequenceiq.flow.core.FlowMetricTag.ROOT_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FAILED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FINISHED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_STARTED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_TIME;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class FlowMetricSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowMetricSender.class);

    private static final String NONE = "NONE";

    @Inject
    private List<? extends AbstractFlowConfiguration> flowConfigurations;

    private Map<String, ? extends AbstractFlowConfiguration> flowConfigurationMap;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    @PostConstruct
    void init() {
        flowConfigurationMap = flowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
    }

    public void send(String flowType, String flowChainType, String nextFlowState, String flowEvent, long startTimeInMillis) {
        try {
            AbstractFlowConfiguration flowConfiguration = flowConfigurationMap.get(flowType);
            String rootFlowChainType = getRootFlowChainType(flowChainType);
            String actualFlowChainType = getActualFlowChainType(flowChainType);
            AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowConfiguration.getEdgeConfig();
            Enum nextFlowStateEnum = FlowStateUtil.getFlowStateEnum(flowConfiguration.getStateType(), nextFlowState, flowEvent);
            if (nextFlowStateEnum == null) {
                LOGGER.debug("nextFlowStateEnum is null for flow type: '{}', flow chain type: '{}', next flow state: '{}', flow event: '{}'," +
                                " flow metrics is not recorded!", flowType, flowChainType, nextFlowState, flowEvent);
            } else if (edgeConfig.getInitState().equals(nextFlowStateEnum)) {
                metricService.incrementMetricCounter(FLOW_STARTED,
                        ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                        ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                        FLOW.name(), flowType);
            } else if (edgeConfig.getFinalState().equals(nextFlowStateEnum)) {
                long duration = System.currentTimeMillis() - startTimeInMillis;
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

    private String getRootFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? StringUtils.substringBefore(flowChainTypes, "/") : NONE;
    }

    private String getActualFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? flowChainTypes.substring(flowChainTypes.lastIndexOf('/') + 1) : NONE;
    }
}
