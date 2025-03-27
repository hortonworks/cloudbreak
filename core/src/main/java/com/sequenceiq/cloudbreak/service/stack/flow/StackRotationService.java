package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackRotationService {

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

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

    private Set<String> getCrnsByParentCrn(String parentCrn) {
        Set<String> crns = Set.of();
        if (CrnResourceDescriptor.getByCrnString(parentCrn).equals(ENVIRONMENT)) {
            crns = stackService.getByEnvironmentCrnAndStackType(parentCrn, StackType.WORKLOAD).stream().map(StackIdView::getCrn).collect(Collectors.toSet());
        } else if (CrnResourceDescriptor.getByCrnString(parentCrn).equals(VM_DATALAKE)) {
            crns = stackService.findNotTerminatedByDatalakeCrn(parentCrn).stream().map(StackIdView::getCrn).collect(Collectors.toSet());
        }
        return crns;
    }
}
