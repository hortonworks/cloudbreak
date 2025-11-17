package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@Service
public class SecretRotationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationValidationService.class);

    @Inject
    private List<SecretType> enabledSecretTypes;

    @Inject
    private SecretRotationStepProgressService secretRotationStepProgressService;

    public void validateEnabledSecretTypes(Collection<SecretType> secretTypes, RotationFlowExecutionType requestedExecutionType) {
        if (requestedExecutionType == null && CollectionUtils.isNotEmpty(enabledSecretTypes) && CollectionUtils.isNotEmpty(secretTypes)) {
            List<SecretType> publicSecretTypes = secretTypes.stream().filter(Predicate.not(SecretType::internal)).toList();
            Collection<SecretType> invalidSecretTypes = CollectionUtils.removeAll(publicSecretTypes, enabledSecretTypes);
            if (!invalidSecretTypes.isEmpty()) {
                throwBadRequest(String.format("Secret types are not enabled: %s", invalidSecretTypes));
            }
        }
    }

    public Optional<RotationFlowExecutionType> validate(String resourceCrn, Collection<SecretType> secretTypes,
            RotationFlowExecutionType rotationFlowExecutionType, Supplier<Boolean> isInAvailableStatePredicate) {
        List<SecretRotationStepProgress> stepProgressList = secretRotationStepProgressService.getProgressList(resourceCrn);
        if (stepProgressList.isEmpty()) {
            validateIfAvailableStatusIsNeeded(secretTypes, isInAvailableStatePredicate);
            validateExecutionType(secretTypes, rotationFlowExecutionType, stepProgressList);
            return Optional.ofNullable(rotationFlowExecutionType);
        } else {
            Optional<SecretRotationStepProgress> failedStep = getFailedRotationStep(stepProgressList);
            if (failedStep.isPresent()) {
                return validateForFailedStep(failedStep.get(), secretTypes, rotationFlowExecutionType);
            } else {
                validateExecutionType(secretTypes, rotationFlowExecutionType, stepProgressList);
                return Optional.ofNullable(rotationFlowExecutionType);
            }
        }
    }

    public boolean failedRotationAlreadyHappened(String resourceCrn, SecretType secretType) {
        List<SecretRotationStepProgress> stepProgressList = secretRotationStepProgressService.getProgressList(resourceCrn);
        if (!stepProgressList.isEmpty()) {
            return stepProgressList.stream()
                    .filter(progress -> progress.getSecretType().equals(secretType))
                    .filter(progress -> Set.of(ROLLBACK, FINALIZE).contains(progress.getCurrentExecutionType()))
                    .anyMatch(progress -> progress.getStatus().equals(SecretRotationStepProgressStatus.FAILED));
        }
        return false;
    }

    private Optional<RotationFlowExecutionType> validateForFailedStep(SecretRotationStepProgress failedStep, Collection<SecretType> secretTypes,
            RotationFlowExecutionType requestedExecutionType) {
        if (!CollectionUtils.isEqualCollection(List.of(failedStep.getSecretType()), secretTypes)) {
            throwBadRequest("There is already a failed secret rotation for %s secret type in %s phase. To resolve the issue please retry secret rotation.",
                    failedStep.getSecretType(), failedStep.getCurrentExecutionType());
        }

        RotationFlowExecutionType previousExecutionType = failedStep.getCurrentExecutionType();
        if (PREVALIDATE.equals(previousExecutionType)) {
            return validateExecutionType(failedStep, requestedExecutionType, PREVALIDATE, requestedExecutionType);
        } else if (ROTATE.equals(previousExecutionType)) {
            return validateExecutionType(failedStep, requestedExecutionType, ROLLBACK, ROLLBACK);
        } else if (ROLLBACK.equals(previousExecutionType)) {
            return validateExecutionType(failedStep, requestedExecutionType, ROLLBACK, ROLLBACK);
        } else {
            return validateExecutionType(failedStep, requestedExecutionType, FINALIZE, FINALIZE);
        }
    }

    private Optional<RotationFlowExecutionType> validateExecutionType(SecretRotationStepProgress failedStep, RotationFlowExecutionType requestedExecutionType,
            RotationFlowExecutionType expectedExecutionType, RotationFlowExecutionType returnedExecutionType) {
        if (requestedExecutionType == null || expectedExecutionType.equals(requestedExecutionType)) {
            return Optional.ofNullable(returnedExecutionType);
        }
        throw new BadRequestException(String.format(
                "There is already a failed secret rotation for %s secret type in %s phase. To resolve the issue please retry secret rotation.",
                failedStep.getSecretType(), failedStep.getCurrentExecutionType()));
    }

    private void validateIfAvailableStatusIsNeeded(Collection<SecretType> secretTypes, Supplier<Boolean> isInAvailableStatePredicate) {
        if (secretTypes.stream().anyMatch(SecretType::statusCheckNeeded) && !isInAvailableStatePredicate.get()) {
            throwBadRequest("The cluster must be in available state to start secret rotation.");
        }
    }

    private Optional<SecretRotationStepProgress> getFailedRotationStep(List<SecretRotationStepProgress> stepProgressList) {
        return stepProgressList.stream()
                .filter(step -> SecretRotationStepProgressStatus.FAILED.equals(step.getStatus()))
                .findFirst();
    }

    private void validateExecutionType(
            Collection<SecretType> secretTypes, RotationFlowExecutionType requestedExecutionType, List<SecretRotationStepProgress> stepProgressList) {
        for (SecretType secretType : secretTypes) {
            Optional<SecretRotationStepProgress> progress = stepProgressList.stream().filter(step -> secretType.equals(step.getSecretType())).findFirst();
            if (progress.isEmpty()) {
                expectPreValidateOrFullExecution(secretType, requestedExecutionType);
            } else if (requestedExecutionType == null) {
                throwBadRequest("There is already a running rotation for %s secret type.", secretType);
            } else {
                switch (requestedExecutionType) {
                    case ROTATE -> expectStepsWithExecutionTypes(progress, PREVALIDATE, ROTATE);
                    case ROLLBACK, FINALIZE -> expectStepsWithExecutionTypes(progress, ROTATE);
                    default -> throwBadRequest("There is already a running rotation for %s secret type.", secretType);
                }
            }
        }
    }

    private void expectPreValidateOrFullExecution(SecretType secretType, RotationFlowExecutionType executionType) {
        if (executionType != null && !PREVALIDATE.equals(executionType)) {
            throwBadRequest("No previous secret rotation is present for %s secret type. API should be called with %s or with null execution type.",
                    secretType, PREVALIDATE);
        }
    }

    private void expectStepsWithExecutionTypes(Optional<SecretRotationStepProgress> progress, RotationFlowExecutionType... expectedExecutionTypes) {
        Optional<RotationFlowExecutionType> executionTypeInDatabase = progress.map(SecretRotationStepProgress::getCurrentExecutionType);
        if (executionTypeInDatabase.isEmpty() || !Arrays.asList(expectedExecutionTypes).contains(executionTypeInDatabase.get())) {
            throwBadRequest("Requested execution type is not allowed based on the progress of the current rotation for the given secret type");
        }
    }

    private void throwBadRequest(String messageTemplate, Object... args) {
        throw new BadRequestException(String.format(messageTemplate, args));
    }
}
