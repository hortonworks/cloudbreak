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

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
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
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private SecretRotationStepProgressService secretRotationProgressService;

    @Inject
    private SecretRotationStatusService secretRotationStatusService;

    @Inject
    private SecretRotationUsageService secretRotationUsageService;

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
    private TransactionService transactionService;

    public void preValidateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, PREVALIDATE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            preValidateService.preValidate(rotationMetadata);
        }
    }

    public void rotateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType, Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROTATE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            secretRotationStatusService.rotationStarted(resourceCrn, secretType);
            secretRotationUsageService.rotationStarted(secretType, resourceCrn, executionType);
            rotationService.rotate(rotationMetadata);
            secretRotationStatusService.rotationFinished(resourceCrn, secretType);
        }
    }

    public void rollbackIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties, Exception rollbackReason) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROLLBACK, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                secretRotationStatusService.rollbackStarted(resourceCrn, secretType, rollbackReason.getMessage());
                secretRotationUsageService.rollbackStarted(secretType, resourceCrn, executionType);
                rollbackService.rollback(rotationMetadata);
                secretRotationProgressService.deleteCurrentRotation(rotationMetadata);
                secretRotationStatusService.rollbackFinished(resourceCrn, secretType);
                secretRotationUsageService.rollbackFinished(secretType, resourceCrn, executionType);
                secretRotationUsageService.rotationFailed(secretType, resourceCrn, rollbackReason.getMessage(), executionType);
            } catch (Exception e) {
                secretRotationStatusService.rollbackFailed(resourceCrn, secretType, e.getMessage());
                secretRotationUsageService.rollbackFailed(secretType, resourceCrn, e.getMessage(), executionType);
                throw e;
            }
        }
    }

    public void finalizeIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType, Map<String, String> additionalProperties) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, FINALIZE, additionalProperties);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                secretRotationStatusService.finalizeStarted(resourceCrn, secretType);
                finalizeService.finalize(rotationMetadata);
                transactionService.required(() -> secretRotationProgressService.deleteCurrentRotation(rotationMetadata));
                secretRotationStatusService.finalizeFinished(resourceCrn, secretType);
                secretRotationUsageService.rotationFinished(secretType, resourceCrn, executionType);
            } catch (TransactionService.TransactionExecutionException te) {
                secretRotationStatusService.finalizeFailed(resourceCrn, secretType, te.getMessage());
                throw new CloudbreakServiceException(te);
            } catch (Exception e) {
                secretRotationStatusService.finalizeFailed(resourceCrn, secretType, e.getMessage());
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
