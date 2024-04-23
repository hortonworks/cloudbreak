package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
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

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.util.FlowStateUtil;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class FreeIpaUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUseCaseMapper.class);

    @Inject
    private List<? extends AbstractFlowConfiguration> flowConfigurations;

    @Inject
    private List<? extends FlowEventChainFactory> flowEventChainFactories;

    private Map<String, ? extends AbstractFlowConfiguration> flowConfigurationMap;

    private Map<String, ? extends FreeIpaUseCaseAware> useCaseAwareFlowConfigurationMap;

    private Map<String, ? extends FreeIpaUseCaseAware> useCaseAwareFlowEventChainFactoryMap;

    @PostConstruct
    void init() {
        flowConfigurationMap = flowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowConfigurationMap = flowConfigurations.stream()
                .filter(FreeIpaUseCaseAware.class::isInstance)
                .map(FreeIpaUseCaseAware.class::cast)
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowEventChainFactoryMap = flowEventChainFactories.stream()
                .filter(FreeIpaUseCaseAware.class::isInstance)
                .map(FreeIpaUseCaseAware.class::cast)
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
        Enum nextFlowState = FlowStateUtil.getFlowStateEnum(flowConfiguration.getStateType(), flow);
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
            FreeIpaUseCaseAware useCaseAwareFlowConfiguration = useCaseAwareFlowConfigurationMap.get(flowType);
            return useCaseAwareFlowConfiguration.getUseCaseForFlowState(nextFlowState);
        }
    }

    private Value getUseCaseFromFlowEventChainFactory(Enum nextFlowState, String rootFlowChainType) {
        if (!useCaseAwareFlowEventChainFactoryMap.containsKey(rootFlowChainType)) {
            LOGGER.debug("Missing use case aware flow event chain factory: {}", rootFlowChainType);
            return UNSET;
        } else {
            FreeIpaUseCaseAware useCaseAwareFlowEventChainFactory = useCaseAwareFlowEventChainFactoryMap.get(rootFlowChainType);
            return useCaseAwareFlowEventChainFactory.getUseCaseForFlowState(nextFlowState);
        }
    }

    private String getRootFlowChainType(String flowChainTypes) {
        if (StringUtils.isNotEmpty(flowChainTypes)) {
            return flowChainTypes.split("/")[0];
        }
        return "";
    }
}
