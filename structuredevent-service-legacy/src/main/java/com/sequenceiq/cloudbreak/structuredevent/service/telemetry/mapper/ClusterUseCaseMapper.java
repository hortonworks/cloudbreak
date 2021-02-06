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
            useCase = lastStepToUseCaseMapping(flow.getFlowState());
        }
        return useCase;
    }

    private UsageProto.CDPClusterStatus.Value firstStepToUseCaseMapping(String flowType) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowType) {
            case "CloudConfigValidationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
                break;
            case "ClusterTerminationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_STARTED;
                break;
            case "StackUpscaleConfig":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_STARTED;
                break;
            case "ClusterDownscaleFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_STARTED;
                break;
            case "StackStartFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_STARTED;
                break;
            case "ClusterStopFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_STARTED;
                break;
            case "SaltUpdateFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED;
                break;
            default:
                LOGGER.debug("Flow type: {}", flowType);
        }
        LOGGER.debug("Mapping flow type to use-case: {}, {}", flowType, useCase);
        return useCase;
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
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
            case "FINALIZE_UPSCALE_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_FINISHED;
                break;
            case "CLUSTER_UPSCALE_FAILED_STATE":
            case "UPSCALE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_FAILED;
                break;
            case "DOWNSCALE_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_FINISHED;
                break;
            case "CLUSTER_DOWNSCALE_FAILED_STATE":
            case "DOWNSCALE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_FAILED;
                break;
            case "START_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_FINISHED;
                break;
            case "CLUSTER_START_FAILED_STATE":
            case "EXTERNAL_DATABASE_START_FAILED_STATE":
            case "START_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_FAILED;
                break;
            case "STOP_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_FINISHED;
                break;
            case "CLUSTER_STOP_FAILED_STATE":
            case "EXTERNAL_DATABASE_STOP_FAILED_STATE":
            case "STOP_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_FAILED;
                break;
            case "CLUSTER_UPGRADE_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED;
                break;
            case "SALT_UPDATE_FAILED_STATE":
            case "CLUSTER_UPGRADE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED;
                break;
            default:
                LOGGER.debug("Flow state: {}", flowState);
        }
        LOGGER.debug("Mapping last flow state to use-case: {}, {}", flowState, useCase);
        return useCase;
    }
    //CHECKSTYLE:ON
}
