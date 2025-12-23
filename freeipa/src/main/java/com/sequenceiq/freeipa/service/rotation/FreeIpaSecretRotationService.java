package com.sequenceiq.freeipa.service.rotation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowEventProvider;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaSecretRotationService implements SecretRotationFlowEventProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSecretRotationService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    @Inject
    private SecretRotationStepProgressService stepProgressService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    public FlowIdentifier rotateSecretsByCrn(String accountId, String environmentCrn, FreeIpaSecretRotationRequest request) {
        LOGGER.info("Requested secret rotation. Account id: {}, environment crn: {}, request: {}", accountId, environmentCrn, request);
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(request.getSecrets(),
                enabledSecretTypes.stream().map(SecretType::getClass).collect(Collectors.toSet()));
        secretRotationValidationService.validateEnabledSecretTypes(secretTypes, null);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);

        Optional<RotationFlowExecutionType> usedExecutionType =
                secretRotationValidationService.validate(environmentCrn, secretTypes, request.getExecutionType(), stack::isAvailable);
        SecretRotationFlowChainTriggerEvent triggerEvent = createSecretRotationTriggerEvent(stack, secretTypes, usedExecutionType.orElse(null),
                request.getAdditionalProperties());
        return flowManager.notify(triggerEvent.selector(), triggerEvent);
    }

    private SecretRotationFlowChainTriggerEvent createSecretRotationTriggerEvent(Stack stack, List<SecretType> secretTypes,
            RotationFlowExecutionType rotationFlowExecutionType, Map<String, String> additionalProperties) {
        return new SecretRotationFlowChainTriggerEvent(
                EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class),
                stack.getId(),
                stack.getEnvironmentCrn(),
                secretTypes,
                rotationFlowExecutionType,
                additionalProperties);
    }

    public void cleanupSecretRotationEntries(String environmentCrn) {
        stepProgressService.deleteAllForResource(environmentCrn);
    }

    @Override
    public Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), true, false, null, true);
    }
}
