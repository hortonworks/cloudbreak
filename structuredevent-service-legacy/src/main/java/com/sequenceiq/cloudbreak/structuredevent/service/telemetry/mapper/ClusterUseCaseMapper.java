package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.metrics.FlowEnumUtil;

@Component
public class ClusterUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUseCaseMapper.class);

    private static final String DISTROX_UPGRADE_REPAIR_FLOWCHAIN = "UpgradeDistroxFlowEventChainFactory/ClusterRepairFlowEventChainFactory";

    @Inject
    private List<? extends AbstractFlowConfiguration> flowConfigurations;

    @Inject
    private List<? extends FlowEventChainFactory> flowEventChainFactories;

    private Map<String, ? extends AbstractFlowConfiguration> flowConfigurationMap;

    private Map<String, ? extends ClusterUseCaseAware> useCaseAwareFlowConfigurationMap;

    private Map<String, ? extends ClusterUseCaseAware> useCaseAwareFlowEventChainFactoryMap;

    @PostConstruct
    void init() {
        flowConfigurationMap = flowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowConfigurationMap = flowConfigurations.stream()
                .filter(ClusterUseCaseAware.class::isInstance)
                .map(ClusterUseCaseAware.class::cast)
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowEventChainFactoryMap = flowEventChainFactories.stream()
                .filter(ClusterUseCaseAware.class::isInstance)
                .map(ClusterUseCaseAware.class::cast)
                .collect(Collectors.toMap(flowEventChainFactory -> flowEventChainFactory.getClass().getSimpleName(), identity()));
    }

    public Value useCase(FlowDetails flow) {
        Value useCase = UNSET;
        if (flow != null) {
            useCase = getUseCase(flow);
        }
        LOGGER.debug("Calculated use-case: {} for flow: {}", useCase, flow);
        return useCase;
    }

    private Value getUseCase(FlowDetails flow) {
        if (StringUtils.isEmpty(flow.getNextFlowState()) || StringUtils.isEmpty(flow.getFlowType())) {
            return UNSET;
        }
        if (!flowConfigurationMap.containsKey(flow.getFlowType())) {
            LOGGER.debug("Missing flow configuration for type: {}", flow.getFlowType());
            return UNSET;
        }
        AbstractFlowConfiguration flowConfiguration = flowConfigurationMap.get(flow.getFlowType());
        Enum nextFlowState = FlowEnumUtil.getFlowStateEnum(flowConfiguration.getStateType(), flow.getNextFlowState(), flow.getFlowEvent());
        if (nextFlowState == null) {
            return UNSET;
        }
        String rootFlowChainType = getRootFlowChainType(flow.getFlowChainType());
        if (StringUtils.isNotEmpty(rootFlowChainType)) {
            return getUseCaseFromFlowEventChainFactory(nextFlowState, rootFlowChainType);
        } else {
            return getUseCaseFromFlowConfiguration(nextFlowState, flow.getFlowType());
        }
    }

    private Value getUseCaseFromFlowConfiguration(Enum nextFlowState, String flowType) {
        if (!useCaseAwareFlowConfigurationMap.containsKey(flowType)) {
            LOGGER.warn("Missing use case aware flow configuration for type: {}", flowType);
            return UNSET;
        } else {
            ClusterUseCaseAware useCaseAwareFlowConfiguration = useCaseAwareFlowConfigurationMap.get(flowType);
            return useCaseAwareFlowConfiguration.getUseCaseForFlowState(nextFlowState);
        }
    }

    private Value getUseCaseFromFlowEventChainFactory(Enum nextFlowState, String rootFlowChainType) {
        if (!useCaseAwareFlowEventChainFactoryMap.containsKey(rootFlowChainType)) {
            LOGGER.debug("Missing use case aware flow event chain factory: {}", rootFlowChainType);
            return UNSET;
        } else {
            ClusterUseCaseAware useCaseAwareFlowEventChainFactory = useCaseAwareFlowEventChainFactoryMap.get(rootFlowChainType);
            return useCaseAwareFlowEventChainFactory.getUseCaseForFlowState(nextFlowState);
        }
    }

    private String getRootFlowChainType(String flowChainTypes) {
        if (StringUtils.isNotEmpty(flowChainTypes)) {
            // In case of Distrox Upgrade the upgrade report and the optional repair report will be separated
            if (flowChainTypes.startsWith(DISTROX_UPGRADE_REPAIR_FLOWCHAIN)) {
                return "ClusterRepairFlowEventChainFactory";
            }
            return flowChainTypes.split("/")[0];
        }
        return "";
    }
}
