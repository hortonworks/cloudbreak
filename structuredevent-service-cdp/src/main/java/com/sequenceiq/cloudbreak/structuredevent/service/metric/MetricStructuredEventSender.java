package com.sequenceiq.cloudbreak.structuredevent.service.metric;

import static com.sequenceiq.flow.core.FlowMetricTag.ACTUAL_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricTag.FLOW;
import static com.sequenceiq.flow.core.FlowMetricTag.ROOT_FLOW_CHAIN;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FAILED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_FINISHED;
import static com.sequenceiq.flow.core.FlowMetricType.FLOW_STARTED;
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
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventSenderService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.util.FlowStateUtil;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class MetricStructuredEventSender implements CDPStructuredEventSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricStructuredEventSender.class);

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

    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        try {
            if (structuredEvent instanceof CDPStructuredFlowEvent) {
                CDPStructuredFlowEvent flowEvent = (CDPStructuredFlowEvent) structuredEvent;
                FlowDetails flow = flowEvent.getFlow();
                if (validateFlowDetails(flow)) {
                    incrementFlowMetrics(flow);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot record flow metrics!", e);
        }
    }

    private void incrementFlowMetrics(FlowDetails flow) {
        AbstractFlowConfiguration flowConfiguration = flowConfigurationMap.get(flow.getFlowType());
        Enum nextFlowState = FlowStateUtil.getFlowStateEnum(flowConfiguration.getStateType(), flow);
        String rootFlowChainType = getRootFlowChainType(flow.getFlowChainType());
        String actualFlowChainType = getActualFlowChainType(flow.getFlowChainType());
        AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowConfiguration.getEdgeConfig();
        if (edgeConfig.getInitState().equals(nextFlowState)) {
            metricService.incrementMetricCounter(FLOW_STARTED,
                    ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                    ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                    FLOW.name(), flow.getFlowType());
        } else if (edgeConfig.getFinalState().equals(nextFlowState)) {
            metricService.incrementMetricCounter(FLOW_FINISHED,
                    ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                    ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                    FLOW.name(), flow.getFlowType());
        } else if (edgeConfig.getDefaultFailureState().equals(nextFlowState)) {
            metricService.incrementMetricCounter(FLOW_FAILED,
                    ROOT_FLOW_CHAIN.name(), rootFlowChainType,
                    ACTUAL_FLOW_CHAIN.name(), actualFlowChainType,
                    FLOW.name(), flow.getFlowType());
        }
    }

    private boolean validateFlowDetails(FlowDetails flow) {
        String flowType = flow.getFlowType();
        if (StringUtils.isEmpty(flow.getNextFlowState()) || StringUtils.isEmpty(flowType)) {
            LOGGER.warn("Flow type or next flow state is null");
            return false;
        }
        if (!flowConfigurationMap.containsKey(flowType)) {
            LOGGER.warn("Missing flow configuration for type: {}", flowType);
            return false;
        }
        AbstractFlowConfiguration flowConfiguration = flowConfigurationMap.get(flowType);
        return FlowStateUtil.getFlowStateEnum(flowConfiguration.getStateType(), flow) != null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private String getRootFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? StringUtils.substringBefore(flowChainTypes, "/") : NONE;
    }

    private String getActualFlowChainType(String flowChainTypes) {
        return StringUtils.isNotEmpty(flowChainTypes) ? flowChainTypes.substring(flowChainTypes.lastIndexOf('/') + 1) : NONE;
    }
}
