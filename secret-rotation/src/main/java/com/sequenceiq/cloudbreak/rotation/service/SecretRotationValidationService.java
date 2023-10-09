package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@Service
public class SecretRotationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationValidationService.class);

    @Inject
    private SecretRotationStepProgressService secretRotationStepProgressService;

    public void validateExecutionType(String resourceCrn, Collection<SecretType> secretTypes, RotationFlowExecutionType requestedExecutionType) {
        for (SecretType secretType : secretTypes) {
            if (!secretType.multiSecret()) {
                Optional<SecretRotationStepProgress> progress = secretRotationStepProgressService.getProgress(resourceCrn, secretType);
                CloudbreakServiceException alreadyRunningRotationException =
                        new CloudbreakServiceException(String.format("There is already a running rotation for %s secret type.", secretType));
                if (progress.isEmpty()) {
                    expectPreValidateOrFullExecution(secretType, requestedExecutionType);
                } else if (requestedExecutionType == null) {
                    throw alreadyRunningRotationException;
                } else {
                    switch (requestedExecutionType) {
                        case ROTATE -> expectStepsWithExecutionTypes(progress, PREVALIDATE);
                        case ROLLBACK, FINALIZE -> expectStepsWithExecutionTypes(progress, ROTATE);
                        default -> throw alreadyRunningRotationException;
                    }
                }
            } else {
                LOGGER.info("Execution type validation skipped in case of secret type {}, since it is related to a multi-cluster rotation, " +
                        "multi-cluster rotation has it's own rules for validation and execution", secretType);
            }
        }
    }

    private void expectPreValidateOrFullExecution(SecretType secretType, RotationFlowExecutionType executionType) {
        if (executionType != null && !PREVALIDATE.equals(executionType)) {
            throw new CloudbreakServiceException(String.format("No previous secret rotation is present for %s secret type. " +
                            "API should be called with %s or with null execution type.", secretType, PREVALIDATE));
        }
    }

    private void expectStepsWithExecutionTypes(Optional<SecretRotationStepProgress> progress, RotationFlowExecutionType... expectedExecutionTypes) {
        Optional<RotationFlowExecutionType> executionTypeInDatabase = progress.map(SecretRotationStepProgress::getCurrentExecutionType);
        if (executionTypeInDatabase.isEmpty() || !Arrays.asList(expectedExecutionTypes).contains(executionTypeInDatabase.get())) {
            throw new CloudbreakServiceException("Requested execution type is not allowed based on the progress " +
                    "of the current rotation for the given secret type");
        }
    }
}
