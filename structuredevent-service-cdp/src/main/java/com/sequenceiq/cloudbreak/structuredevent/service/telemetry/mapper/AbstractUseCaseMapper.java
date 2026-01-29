package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.metrics.FlowEnumUtil;

public abstract class AbstractUseCaseMapper<T, R extends UseCaseAware<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUseCaseMapper.class);

    @Inject
    private List<? extends AbstractFlowConfiguration> flowConfigurations;

    @Inject
    private List<? extends FlowEventChainFactory> flowEventChainFactories;

    private Map<String, ? extends AbstractFlowConfiguration> flowConfigurationMap;

    private Map<String, R> useCaseAwareFlowConfigurationMap;

    private Map<String, R> useCaseAwareFlowEventChainFactoryMap;

    protected abstract T unset();

    protected abstract Class<R> useCaseAwareClass();

    @PostConstruct
    void init() {
        flowConfigurationMap = flowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowConfigurationMap = flowConfigurations.stream()
                .filter(useCaseAwareClass()::isInstance)
                .map(useCaseAwareClass()::cast)
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowEventChainFactoryMap = flowEventChainFactories.stream()
                .filter(useCaseAwareClass()::isInstance)
                .map(useCaseAwareClass()::cast)
                .collect(Collectors.toMap(flowEventChainFactory -> flowEventChainFactory.getClass().getSimpleName(), identity()));
    }

    public T useCase(FlowDetails flow) {
        T useCase = unset();
        if (flow != null) {
            useCase = getUseCase(flow);
        }
        LOGGER.debug("Calculated use-case: {} for flow: {}", useCase, flow);
        return useCase;
    }

    private T getUseCase(FlowDetails flow) {
        if (StringUtils.isEmpty(flow.getNextFlowState()) || StringUtils.isEmpty(flow.getFlowType())) {
            return unset();
        }
        if (!flowConfigurationMap.containsKey(flow.getFlowType())) {
            LOGGER.debug("Missing flow configuration for type: {}", flow.getFlowType());
            return unset();
        }
        AbstractFlowConfiguration flowConfiguration = flowConfigurationMap.get(flow.getFlowType());
        Enum nextFlowState = FlowEnumUtil.getFlowStateEnum(flowConfiguration.getStateType(), flow.getNextFlowState(), flow.getFlowEvent());
        if (nextFlowState == null) {
            LOGGER.debug("Missing flow state enum for type: {}, state: {}", flowConfiguration.getStateType(), flow.getNextFlowState());
            return unset();
        }
        Optional<String> rootFlowChainType = getRootFlowChainType(flow.getFlowChainType());
        if (rootFlowChainType.isPresent()) {
            return getUseCaseFromFlowEventChainFactory(nextFlowState, rootFlowChainType.get());
        } else {
            return getUseCaseFromFlowConfiguration(nextFlowState, flow.getFlowType());
        }
    }

    private T getUseCaseFromFlowConfiguration(Enum nextFlowState, String flowType) {
        if (!useCaseAwareFlowConfigurationMap.containsKey(flowType)) {
            LOGGER.debug("Missing use case aware flow configuration for type: {}", flowType);
            return unset();
        } else {
            R useCaseAwareFlowConfiguration = useCaseAwareFlowConfigurationMap.get(flowType);
            return (T) useCaseAwareFlowConfiguration.getUseCaseForFlowState(nextFlowState);
        }
    }

    private T getUseCaseFromFlowEventChainFactory(Enum nextFlowState, String rootFlowChainType) {
        if (!useCaseAwareFlowEventChainFactoryMap.containsKey(rootFlowChainType)) {
            LOGGER.debug("Missing use case aware flow event chain factory: {}", rootFlowChainType);
            return unset();
        } else {
            R useCaseAwareFlowEventChainFactory = useCaseAwareFlowEventChainFactoryMap.get(rootFlowChainType);
            return (T) useCaseAwareFlowEventChainFactory.getUseCaseForFlowState(nextFlowState);
        }
    }

    private Optional<String> getRootFlowChainType(String flowChainTypes) {
        if (StringUtils.isNotEmpty(flowChainTypes)) {
            return Optional.ofNullable(flowChainTypes.split("/")[0]);
        }
        return Optional.empty();
    }
}
