package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackRotationService {

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

}
