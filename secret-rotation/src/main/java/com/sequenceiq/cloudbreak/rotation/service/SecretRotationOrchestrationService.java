package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.MultiClusterRotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationCleanupService;
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
    private MultiClusterRotationCleanupService multiClusterRotationCleanupService;

    @Inject
    private SecretRotationPreValidateService preValidateService;

    @Inject
    private SecretRotationService rotationService;

    @Inject
    private SecretRotationRollbackService rollbackService;

    @Inject
    private SecretRotationFinalizeService finalizeService;

    public void preValidateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROTATE);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            preValidateService.preValidate(rotationMetadata);
        }
    }

    public void rotateIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROTATE);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            secretRotationStatusService.rotationStarted(resourceCrn, secretType);
            secretRotationUsageService.rotationStarted(secretType, resourceCrn, executionType);
            rotationService.rotate(rotationMetadata);
            secretRotationStatusService.rotationFinished(resourceCrn, secretType);
            secretRotationProgressService.deleteAllForCurrentExecution(rotationMetadata);
        }
    }

    public void rollbackIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType, SecretRotationStep failedStep) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, ROLLBACK);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                secretRotationStatusService.rollbackStarted(resourceCrn, secretType);
                secretRotationUsageService.rollbackStarted(secretType, resourceCrn, executionType);
                rollbackService.rollback(rotationMetadata, failedStep);
                secretRotationStatusService.rollbackFinished(resourceCrn, secretType);
                secretRotationUsageService.rollbackFinished(secretType, resourceCrn, executionType);
                secretRotationProgressService.deleteAllForCurrentExecution(rotationMetadata);
                rotationMetadata.multiClusterRotationMetadata().ifPresent(multiSecretRotationMetadata ->
                        multiClusterRotationCleanupService.cleanupAfterRollback(resourceCrn, multiSecretRotationMetadata));
            } catch (Exception e) {
                secretRotationStatusService.rollbackFailed(resourceCrn, secretType, e.getMessage());
                secretRotationUsageService.rollbackFailed(secretType, resourceCrn, e.getMessage(), executionType);
                secretRotationProgressService.deleteAllForCurrentExecution(rotationMetadata);
                throw e;
            }
        }
    }

    public void finalizeIfNeeded(SecretType secretType, String resourceCrn, RotationFlowExecutionType executionType) {
        RotationMetadata rotationMetadata = getRotationMetadata(secretType, resourceCrn, executionType, FINALIZE);
        if (executionDecisionProvider.executionRequired(rotationMetadata)) {
            try {
                secretRotationStatusService.finalizeStarted(resourceCrn, secretType);
                finalizeService.finalize(rotationMetadata);
                secretRotationStatusService.finalizeFinished(resourceCrn, secretType);
                secretRotationUsageService.rotationFinished(secretType, resourceCrn, executionType);
                secretRotationProgressService.deleteAllForCurrentExecution(rotationMetadata);
            } catch (Exception e) {
                secretRotationStatusService.finalizeFailed(resourceCrn, secretType, e.getMessage());
                secretRotationProgressService.deleteAllForCurrentExecution(rotationMetadata);
                throw e;
            }
        }
        rotationMetadata.multiClusterRotationMetadata().ifPresent(multiSecretRotationMetadata ->
                multiClusterRotationCleanupService.cleanupAfterFinalize(resourceCrn, multiSecretRotationMetadata));
    }

    private RotationMetadata getRotationMetadata(SecretType secretType, String resourceCrn, RotationFlowExecutionType requestedExecutionType,
            RotationFlowExecutionType currentExecutionType) {
        RotationContextProvider rotationContextProvider = rotationContextProviderMap.get(secretType);
        return getRotationMetadata(secretType, currentExecutionType, requestedExecutionType, resourceCrn, rotationContextProvider);
    }

    private RotationMetadata getRotationMetadata(SecretType secretType, RotationFlowExecutionType currentExecution,
            RotationFlowExecutionType requestExecutionType, String resourceCrn, RotationContextProvider rotationContextProvider) {
        RotationMetadata.Builder builder = RotationMetadata.builder()
                .secretType(secretType)
                .currentExecution(currentExecution)
                .requestedExecutionType(requestExecutionType)
                .resourceCrn(resourceCrn);
        if (secretType.multiSecret()) {
            builder.multiClusterRotationMetadata(getMultiClusterRotationContextProvider(rotationContextProvider).getMultiClusterRotationMetadata(resourceCrn));
        }
        return builder.build();
    }

    public static MultiClusterRotationContextProvider getMultiClusterRotationContextProvider(RotationContextProvider rotationContextProvider) {
        if (rotationContextProvider instanceof MultiClusterRotationContextProvider) {
            return (MultiClusterRotationContextProvider) rotationContextProvider;
        } else {
            throw new RuntimeException("There is no metadata for multi cluster secret rotation!");
        }
    }
}