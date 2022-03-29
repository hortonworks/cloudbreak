package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

@Component
public class EnvironmentUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUseCaseMapper.class);

    @Inject
    private CDPRequestProcessingStepMapper cdpRequestProcessingStepMapper;

    // At the moment we need to introduce a complex logic to figure out the use case
    public UsageProto.CDPEnvironmentStatus.Value useCase(FlowDetails flow) {
        UsageProto.CDPEnvironmentStatus.Value useCase = UsageProto.CDPEnvironmentStatus.Value.UNSET;
        if (flow != null) {
            if (cdpRequestProcessingStepMapper.isFirstStep(flow)) {
                if (flow.getFlowType() != null) {
                    useCase = firstStepToUseCaseMapping(flow.getFlowType());
                }
            } else if (cdpRequestProcessingStepMapper.isLastStep(flow)) {
                useCase = lastStepToUseCaseMapping(flow.getNextFlowState());
            }
        }
        return useCase;
    }

    private UsageProto.CDPEnvironmentStatus.Value firstStepToUseCaseMapping(String flowType) {
        UsageProto.CDPEnvironmentStatus.Value useCase = UsageProto.CDPEnvironmentStatus.Value.UNSET;
        switch (flowType) {
            case "EnvCreationFlowConfig":
                useCase = UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED;
                break;
            case "EnvDeleteFlowConfig":
                useCase = UsageProto.CDPEnvironmentStatus.Value.DELETE_STARTED;
                break;
            case "EnvStartFlowConfig":
                useCase = UsageProto.CDPEnvironmentStatus.Value.RESUME_STARTED;
                break;
            case "EnvStopFlowConfig":
                useCase = UsageProto.CDPEnvironmentStatus.Value.SUSPEND_STARTED;
                break;
            default:
                LOGGER.debug("Flow type: {}", flowType);
        }
        LOGGER.debug("Mapping flow type to use-case: {}, {}", flowType, useCase);
        return useCase;
    }

    private UsageProto.CDPEnvironmentStatus.Value lastStepToUseCaseMapping(String nextFlowState) {
        UsageProto.CDPEnvironmentStatus.Value useCase = UsageProto.CDPEnvironmentStatus.Value.UNSET;
        switch (nextFlowState) {
            case "ENV_CREATION_FINISHED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED;
                break;
            case "ENV_CREATION_FAILED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.CREATE_FAILED;
                break;
            case "ENV_DELETE_FINISHED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.DELETE_FINISHED;
                break;
            case "ENV_DELETE_FAILED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.DELETE_FAILED;
                break;
            case "ENV_START_FINISHED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.RESUME_FINISHED;
                break;
            case "ENV_START_FAILED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.RESUME_FAILED;
                break;
            case "ENV_STOP_FINISHED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FINISHED;
                break;
            case "ENV_STOP_FAILED_STATE":
                useCase = UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FAILED;
                break;
            default:
                LOGGER.debug("Next flow state: {}", nextFlowState);
        }
        LOGGER.debug("Mapping next flow state to use-case: {}, {}", nextFlowState, useCase);
        return useCase;
    }
}