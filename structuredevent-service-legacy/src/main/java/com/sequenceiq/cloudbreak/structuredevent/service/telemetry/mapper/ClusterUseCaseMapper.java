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
    private ClusterRequestProcessingStepMapper clusterRequestProcessingStepMapper;

    // At the moment we need to introduce a complex logic to figure out the use case
    public UsageProto.CDPClusterStatus.Value useCase(FlowDetails flow) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        if (clusterRequestProcessingStepMapper.isFirstStep(flow)) {
            useCase = firstStepToUseCaseMapping(flow.getFlowType());
        } else if (clusterRequestProcessingStepMapper.isLastStep(flow)) {
            String flowState = flow.getFlowState();
            useCase = lastStepToUseCaseMapping(flowState);
        }
        return useCase;
    }

    private UsageProto.CDPClusterStatus.Value firstStepToUseCaseMapping(String flowType) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowType) {
            case "CloudConfigValidationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
                break;
            default:
                LOGGER.debug("Flow type: {}", flowType);
        }
        LOGGER.debug("Mapping flow type to use-case: {}, {}", flowType, useCase);
        return useCase;
    }

    private UsageProto.CDPClusterStatus.Value lastStepToUseCaseMapping(String flowState) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowState) {
            case "CLUSTER_CREATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
                break;
            case "VALIDATE_CLOUD_CONFIG_FAILED_STATE":
            case "VALIDATE_KERBEROS_CONFIG_FAILED_STATE":
            case "EXTERNAL_DATABASE_CREATION_FAILED_STATE":
            case "CLUSTER_CREATION_FAILED_STATE":
            case "STACK_CREATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
                break;
            case "TERMINATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FINISHED;
                break;
            case "CLUSTER_TERMINATION_FAILED_STATE":
            case "EXTERNAL_DATABASE_TERMINATION_FAILED_STATE":
            case "TERMINATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FAILED;
                break;
            default:
                LOGGER.debug("Flow state: {}", flowState);
        }
        LOGGER.debug("Mapping last flow state to use-case: {}, {}", flowState, useCase);
        return useCase;
    }
}
