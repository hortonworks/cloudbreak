package com.sequenceiq.cloudbreak.service.salt;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class RotateSaltPasswordTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordTriggerService.class);

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    @Inject
    private SecretRotationStepProgressService secretRotationStepProgressService;

    public FlowIdentifier triggerRotateSaltPassword(StackDto stack, RotateSaltPasswordReason reason) {
        if (secretRotationValidationService.failedRotationAlreadyHappened(stack.getResourceCrn(), CloudbreakSecretType.SALT_PASSWORD)) {
            secretRotationStepProgressService.deleteCurrentRotation(RotationMetadata.builder()
                    .secretType(CloudbreakSecretType.SALT_PASSWORD)
                    .resourceCrn(stack.getResourceCrn())
                    .build());
            LOGGER.info("Since there is already a failed salt password rotation for stack {}, " +
                    "we are doing salt update to initiate a salt rebootstrap and resolve the issue with saltuser.", stack.getResourceCrn());
            return reactorFlowManager.triggerSaltUpdate(stack.getId());
        } else {
            LOGGER.info("Triggering rotate salt password for stack {}", stack.getResourceCrn());
            return stackRotationService.rotateSecrets(stack.getResourceCrn(), List.of(CloudbreakSecretType.SALT_PASSWORD.value()), null, null);
        }
    }
}
