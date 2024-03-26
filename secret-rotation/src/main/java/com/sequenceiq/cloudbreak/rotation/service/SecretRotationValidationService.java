package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@Service
public class SecretRotationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationValidationService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SecretRotationStepProgressService secretRotationStepProgressService;

    @Inject
    private MultiClusterRotationValidationService multiClusterRotationValidationService;

    public void validateSecretRotationEntitlement(String resourceCrn) {
        if (!entitlementService.isSecretRotationEnabled(Crn.safeFromString(resourceCrn).getAccountId())) {
            throwBadRequest("Account is not entitled to execute secret rotation.");
        }
    }

    public Optional<RotationFlowExecutionType> validate(String resourceCrn, Collection<SecretType> secretTypes,
            RotationFlowExecutionType rotationFlowExecutionType, Supplier<Boolean> isInAvailableStatePredicate) {
        validateMultiSecretRotation(resourceCrn, secretTypes);
        List<SecretRotationStepProgress> stepProgressList = secretRotationStepProgressService.getProgressList(resourceCrn);
        if (stepProgressList.isEmpty()) {
            validateIfAvailableStatusIsNeeded(secretTypes, isInAvailableStatePredicate);
            validateExecutionType(secretTypes, rotationFlowExecutionType, stepProgressList);
            return Optional.ofNullable(rotationFlowExecutionType);
        } else {
            Optional<SecretRotationStepProgress> failedStep = getFailedRotationFlowStep(stepProgressList);
            if (failedStep.isPresent()) {
                return validateForFailedStep(failedStep.get(), secretTypes, rotationFlowExecutionType);
            } else {
                validateExecutionType(secretTypes, rotationFlowExecutionType, stepProgressList);
                return Optional.ofNullable(rotationFlowExecutionType);
            }
        }
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

    private void validateMultiSecretRotation(String resourceCrn, Collection<SecretType> secretTypes) {
        secretTypes.stream()
                .filter(SecretType::multiSecret)
                .forEach(secretType -> multiClusterRotationValidationService.validateMultiRotationRequest(resourceCrn, secretType));
    }

    private void validateIfAvailableStatusIsNeeded(Collection<SecretType> secretTypes, Supplier<Boolean> isInAvailableStatePredicate) {
        if (secretTypes.stream().anyMatch(Predicate.not(SecretType::multiSecret)) && !isInAvailableStatePredicate.get()) {
            throwBadRequest("The cluster must be in available state to start secret rotation.");
        }
    }

    private Optional<SecretRotationStepProgress> getFailedRotationFlowStep(List<SecretRotationStepProgress> stepProgressList) {
        return stepProgressList.stream()
                .filter(step -> SecretRotationStepProgressStatus.FAILED.equals(step.getStatus()))
                .findFirst();
    }

    private void validateExecutionType(
            Collection<SecretType> secretTypes, RotationFlowExecutionType requestedExecutionType, List<SecretRotationStepProgress> stepProgressList) {
        for (SecretType secretType : secretTypes) {
            if (!secretType.multiSecret()) {
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
            } else {
                LOGGER.info("Execution type validation skipped in case of secret type {}, since it is related to a multi-cluster rotation, " +
                        "multi-cluster rotation has it's own rules for validation and execution", secretType);
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
