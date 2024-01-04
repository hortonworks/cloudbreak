package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value.UNSET;
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

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentStatus.Value;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class EnvironmentUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUseCaseMapper.class);

    @Inject
    private List<? extends AbstractFlowConfiguration> flowConfigurations;

    private Map<String, ? extends AbstractFlowConfiguration> flowConfigurationMap;

    private Map<String, ? extends EnvironmentUseCaseAware> useCaseAwareFlowConfigurationMap;

    @PostConstruct
    void init() {
        flowConfigurationMap = flowConfigurations.stream()
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
        useCaseAwareFlowConfigurationMap = flowConfigurations.stream()
                .filter(EnvironmentUseCaseAware.class::isInstance)
                .map(EnvironmentUseCaseAware.class::cast)
                .collect(Collectors.toMap(flowConfiguration -> flowConfiguration.getClass().getSimpleName(), identity()));
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
        Enum nextFlowState = getFlowStateEnum(flowConfiguration.getStateType(), flow.getNextFlowState());
        if (nextFlowState == null) {
            LOGGER.warn("Missing flow state enum for type: {}, state: {}", flowConfiguration.getStateType(), flow.getNextFlowState());
            return UNSET;
        }
        return getUseCaseFromFlowConfiguration(nextFlowState, flow.getFlowType());
    }

    private Value getUseCaseFromFlowConfiguration(Enum nextFlowState, String flowType) {
        if (!useCaseAwareFlowConfigurationMap.containsKey(flowType)) {
            LOGGER.warn("Missing use case aware flow configuration for type: {}", flowType);
            return UNSET;
        } else {
            EnvironmentUseCaseAware useCaseAwareFlowConfiguration = useCaseAwareFlowConfigurationMap.get(flowType);
            return useCaseAwareFlowConfiguration.getUseCaseForFlowState(nextFlowState);
        }
    }

    private Enum<? extends FlowState> getFlowStateEnum(Class<? extends Enum> stateClass, String stateValue) {
        if (StringUtils.isEmpty(stateValue) || stateClass == null) {
            return null;
        }
        try {
            return Enum.valueOf(stateClass, stateValue);
        } catch (Exception e) {
            LOGGER.warn("Cannot get enum for class: {}, value: {}", stateClass, stateValue, e);
            return null;
        }
    }
}
