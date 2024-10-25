package com.sequenceiq.cloudbreak.service.salt;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class RotateSaltPasswordTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordTriggerService.class);

    @Inject
    private StackRotationService stackRotationService;

    public FlowIdentifier triggerRotateSaltPassword(StackDto stack, RotateSaltPasswordReason reason) {
        LOGGER.info("Triggering rotate salt password for stack {}", stack.getResourceCrn());
        return stackRotationService.rotateSecrets(stack.getResourceCrn(), List.of(CloudbreakSecretType.SALT_PASSWORD.value()), null, null);
    }
}
