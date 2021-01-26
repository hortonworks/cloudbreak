package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

@Component
public class ClusterUseCaseMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUseCaseMapper.class);

    @Inject
    private RequestProcessingStepMapper requestProcessingStepMapper;

    // At the moment we need to introduce a complex logic to figure out the use case
    public UsageProto.CDPClusterStatus.Value useCase(FlowDetails flow) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        if (requestProcessingStepMapper.isFirstStep(flow)) {
            String flowEvent = flow.getFlowEvent();
            useCase = firstStepToUseCaseMapping(flowEvent);
        } else if (requestProcessingStepMapper.isLastStep(flow)) {
            String flowState = flow.getFlowState();
            useCase = lastStepToUseCaseMapping(flowState);
        }
        return useCase;
    }

    private UsageProto.CDPClusterStatus.Value firstStepToUseCaseMapping(String flowEvent) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowEvent) {
            case "START_ENVIRONMENT_INITIALIZATION_EVENT":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
                break;
            default:
                LOGGER.debug("Flow state: {}", flowEvent);
        }
        LOGGER.debug("Mapping flow event to use-case: {}, {}", flowEvent, useCase);
        return useCase;
    }

    private UsageProto.CDPClusterStatus.Value lastStepToUseCaseMapping(String flowState) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowState) {
            case "ENV_CREATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
                break;
            case "ENV_CREATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
                break;
            case "ENV_DELETE_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FINISHED;
                break;
            case "ENV_DELETE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FAILED;
                break;
            default:
                LOGGER.debug("Flow state: {}", flowState);
        }
        LOGGER.debug("Mapping last flow state to use-case: {}, {}", flowState, useCase);
        return useCase;
    }
}
