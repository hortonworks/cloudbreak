package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.ConditionalRotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationProgressFallback;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupDescriptor;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressResponse;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@Service
public class StackRotationService {

    private static final Logger LOGGER = getLogger(StackRotationService.class);

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SecretRotationStepProgressService stepProgressService;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private Map<SecretType, ConditionalRotationContextProvider> conditionalRotationContextProviderMap;

    @Inject
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public FlowIdentifier rotateSecrets(String crn, List<String> secrets, RotationFlowExecutionType requestedExecutionType,
            Map<String, String> additionalProperties) {
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets,
                enabledSecretTypes.stream().map(SecretType::getClass).collect(Collectors.toSet()));
        secretRotationValidationService.validateEnabledSecretTypes(secretTypes, requestedExecutionType);
        StackDto stackDto = stackDtoService.getByCrn(crn);
        List<SecretType> filteredSecretTypes = secretTypes.stream()
                .filter(type -> !conditionalRotationContextProviderMap.containsKey(type) ||
                        conditionalRotationContextProviderMap.get(type).isApplicable(stackDto))
                .toList();
        if (filteredSecretTypes.isEmpty()) {
            throw new BadRequestException("None of the requested secret types are applicable for the cluster!");
        }
        Optional<RotationFlowExecutionType> usedExecutionType =
                secretRotationValidationService.validate(crn, filteredSecretTypes, requestedExecutionType, stackDto::isAvailable);
        return flowManager.triggerSecretRotation(stackDto.getId(), crn, filteredSecretTypes, usedExecutionType.orElse(null), additionalProperties);
    }

    public void cleanupSecretRotationEntries(String crn) {
        stepProgressService.deleteAllForResource(crn);
    }

    public StepProgressResponse getProgressResponse(String crn, String secret) {
        SecretType secretType = SecretTypeConverter.mapSecretType(secret,
                Arrays.stream(CloudbreakSecretType.values()).map(SecretType::getClass).collect(Collectors.toSet()));
        try {
            return stepProgressService.getProgressResponse(crn, secretType);
        } catch (NotFoundException e) {
            LOGGER.debug("Local progress not found for '{}' / '{}', falling back to polling types.", crn, secret);
            return getProgressFromPollingTypes(crn, secretType)
                    .orElseThrow(() -> e);
        }
    }

    private Optional<StepProgressResponse> getProgressFromPollingTypes(String crn, SecretType secretType) {
        return resolvePollingFanOut(crn, secretType)
                .flatMap(pollingFanOut -> SecretRotationProgressFallback.findFirstProgress(pollingFanOut.pollingTypes(),
                        (source, perSourceType) -> {
                            String targetCrn = resolveTargetCrn(source, crn, pollingFanOut.stack());
                            return targetCrn == null ? null : getProgressFromSource(source, targetCrn, perSourceType);
                        }));
    }

    private Optional<PollingFanOut> resolvePollingFanOut(String crn, SecretType secretType) {
        if (Collections.disjoint(secretType.getSteps(), Set.of(FREEIPA_ROTATE_POLLING, REDBEAMS_ROTATE_POLLING)) ||
                !rotationContextProviderMap.containsKey(secretType)) {
            return Optional.empty();
        }
        try {
            StackDto stack = stackDtoService.getByCrn(crn);
            return Optional.of(new PollingFanOut(stack, rotationContextProviderMap.get(secretType).getPollingTypes()));
        } catch (NotFoundException nfe) {
            LOGGER.info("Stack by crn {} does not exists.", crn);
            return Optional.empty();
        }
    }

    private String resolveTargetCrn(RotationSource source, String crn, StackDto stack) {
        return switch (source) {
            case FREEIPA -> stack.getEnvironmentCrn();
            case REDBEAMS -> stack.getCluster().getDatabaseServerCrn();
            case DATALAKE, CLOUDBREAK -> crn;
        };
    }

    private StepProgressResponse getProgressFromSource(RotationSource source, String targetCrn, SecretType secretType) {
        return switch (source) {
            case FREEIPA -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> freeIpaRotationV1Endpoint.getProgress(secretType.value(), targetCrn));
            case REDBEAMS -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> databaseServerV4Endpoint.getSecretRotationProgress(secretType.value(), targetCrn));
            case DATALAKE, CLOUDBREAK -> null;
        };
    }

    public List<StepProgressCleanupDescriptor> cleanupProgress(String crn, String secret) {
        SecretType secretType = SecretTypeConverter.mapSecretType(secret,
                Arrays.stream(CloudbreakSecretType.values()).map(SecretType::getClass).collect(Collectors.toSet()));
        StepProgressCleanupDescriptor currentStepProgressCleanupDescriptor = stepProgressService.delete(crn, secretType, RotationSource.CLOUDBREAK);
        List<StepProgressCleanupDescriptor> furtherRequiredCleanupDescriptors = collectUnderlyingSecretsForProgressCleanup(crn, secretType);
        return ListUtils.union(List.of(currentStepProgressCleanupDescriptor), furtherRequiredCleanupDescriptors);
    }

    public List<StepProgressCleanupDescriptor> collectUnderlyingSecretsForProgressCleanup(String crn, SecretType secretType) {
        return resolvePollingFanOut(crn, secretType)
                .map(pollingFanOut -> pollingFanOut.pollingTypes().entrySet().stream()
                        .map(entry -> StepProgressCleanupDescriptor.of(entry.getKey(), StepProgressCleanupStatus.PENDING,
                                resolveTargetCrn(entry.getKey(), crn, pollingFanOut.stack()),
                                SecretRotationEnumSerializationUtil.serialize(entry.getValue())))
                        .filter(descriptor -> descriptor.crn() != null)
                        .toList())
                .orElseGet(List::of);
    }

    private record PollingFanOut(StackDto stack, Map<RotationSource, SecretType> pollingTypes) {
    }
}
