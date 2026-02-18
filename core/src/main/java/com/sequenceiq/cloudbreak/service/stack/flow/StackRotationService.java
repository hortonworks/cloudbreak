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

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupDescriptor;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressCleanupStatus;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

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

    public FlowIdentifier rotateSecrets(String crn, List<String> secrets, RotationFlowExecutionType requestedExecutionType,
            Map<String, String> additionalProperties) {
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets,
                enabledSecretTypes.stream().map(SecretType::getClass).collect(Collectors.toSet()));
        secretRotationValidationService.validateEnabledSecretTypes(secretTypes, requestedExecutionType);
        StackView stack = stackDtoService.getStackViewByCrn(crn);
        Optional<RotationFlowExecutionType> usedExecutionType =
                secretRotationValidationService.validate(crn, secretTypes, requestedExecutionType, stack::isAvailable);
        return flowManager.triggerSecretRotation(stack.getId(), crn, secretTypes, usedExecutionType.orElse(null), additionalProperties);
    }

    public void cleanupSecretRotationEntries(String crn) {
        stepProgressService.deleteAllForResource(crn);
    }

    public List<StepProgressCleanupDescriptor> cleanupProgress(String crn, String secret) {
        SecretType secretType = SecretTypeConverter.mapSecretType(secret,
                Arrays.stream(CloudbreakSecretType.values()).map(SecretType::getClass).collect(Collectors.toSet()));
        StepProgressCleanupDescriptor currentStepProgressCleanupDescriptor = stepProgressService.delete(crn, secretType, RotationSource.CLOUDBREAK);
        List<StepProgressCleanupDescriptor> furtherRequiredCleanupDescriptors = collectUnderlyingSecretsForProgressCleanup(crn, secretType);
        return ListUtils.union(List.of(currentStepProgressCleanupDescriptor), furtherRequiredCleanupDescriptors);
    }

    public List<StepProgressCleanupDescriptor> collectUnderlyingSecretsForProgressCleanup(String crn, SecretType secretType) {
        try {
            if (!Collections.disjoint(secretType.getSteps(), Set.of(FREEIPA_ROTATE_POLLING, REDBEAMS_ROTATE_POLLING)) &&
                    rotationContextProviderMap.containsKey(secretType)) {
                Map<RotationSource, SecretType> pollingTypes = rotationContextProviderMap.get(secretType).getPollingTypes();
                StackDto stack = stackDtoService.getByCrn(crn);
                return pollingTypes.entrySet().stream().map(entry -> {
                    String targetCrn = switch (entry.getKey()) {
                        case FREEIPA -> stack.getEnvironmentCrn();
                        case REDBEAMS -> stack.getCluster().getDatabaseServerCrn();
                        case DATALAKE, CLOUDBREAK -> crn;
                    };
                    return StepProgressCleanupDescriptor.of(entry.getKey(), StepProgressCleanupStatus.PENDING, targetCrn,
                            SecretRotationEnumSerializationUtil.serialize(entry.getValue()));
                }).filter(descriptor -> descriptor.crn() != null).toList();
            }
        } catch (NotFoundException nfe) {
            LOGGER.info("Stack by crn {} does not exists.", crn);
        }
        return List.of();
    }

}
