package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationFinalizeService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationPreValidateService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationRollbackService;
import com.sequenceiq.cloudbreak.rotation.service.phase.SecretRotationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.rotation.service.usage.SecretRotationUsageService;

@Service
public class SecretRotationOrchestrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationOrchestrationService.class);

    @Inject
    private SecretRotationStepProgressService progressService;

    @Inject
    private SecretRotationStatusService statusService;

    @Inject
    private SecretRotationUsageService usageService;

    @Inject
    private SecretRotationExecutionDecisionProvider executionDecisionProvider;

    @Inject
    private SecretRotationPreValidateService preValidateService;

    @Inject
    private SecretRotationService rotationService;

    @Inject
    private SecretRotationRollbackService rollbackService;

    @Inject
    private SecretRotationFinalizeService finalizeService;

    @Inject
    private SecretRotationHistoryService historyService;

    public void preValidateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, PREVALIDATE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                preValidateService.preValidate(rotationMetadata);
            } catch (Exception e) {
                // for prevalidation we do not need to store failed progress in DB
                progressService.deleteCurrentRotation(rotationMetadata);
                throw e;
            }
        }
    }

    public void rotateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType, Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROTATE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            statusService.rotationStarted(resourceCrn, secretType);
            usageService.rotationStarted(secretType, resourceCrn, executionType);
            rotationService.rotate(rotationMetadata);
            statusService.rotationFinished(resourceCrn, secretType);
        }
    }

    public void rollbackIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties, Exception rollbackReason) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROLLBACK, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                statusService.rollbackStarted(resourceCrn, secretType, rollbackReason.getMessage());
                usageService.rollbackStarted(secretType, resourceCrn, executionType);
                rollbackService.rollback(rotationMetadata);
                progressService.deleteCurrentRotation(rotationMetadata);
                statusService.rollbackFinished(resourceCrn, secretType);
                usageService.rollbackFinished(secretType, resourceCrn, executionType);
                usageService.rotationFailed(secretType, resourceCrn, rollbackReason.getMessage(), executionType);
            } catch (Exception e) {
                statusService.rollbackFailed(resourceCrn, secretType, e.getMessage());
                usageService.rollbackFailed(secretType, resourceCrn, e.getMessage(), executionType);
                throw e;
            }
        }
    }

    public void finalizeIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType, Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, FINALIZE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                statusService.finalizeStarted(resourceCrn, secretType);
                finalizeService.finalize(rotationMetadata);
                progressService.deleteCurrentRotation(rotationMetadata);
                historyService.addHistoryItem(rotationMetadata);
                statusService.finalizeFinished(resourceCrn, secretType);
                usageService.rotationFinished(secretType, resourceCrn, executionType);
            } catch (Exception e) {
                statusService.finalizeFailed(resourceCrn, secretType, e.getMessage());
                throw e;
            }
        }
    }

    private RotationMetadata getRotationMetadata(SecretType secretType, String resourceCrn, RotationFlowExecutionType requestedExecutionType,
            RotationFlowExecutionType currentExecutionType, Map<String, String> additionalProperties) {
        RotationMetadata.Builder builder = RotationMetadata.builder()
                .secretType(secretType)
                .currentExecution(currentExecutionType)
                .requestedExecutionType(requestedExecutionType)
                .resourceCrn(resourceCrn)
                .additionalProperties(additionalProperties);
        return builder.build();
    }
}
