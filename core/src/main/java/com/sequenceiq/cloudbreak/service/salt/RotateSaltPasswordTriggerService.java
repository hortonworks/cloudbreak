package com.sequenceiq.cloudbreak.service.salt;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordType;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class RotateSaltPasswordTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordTriggerService.class);

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    public FlowIdentifier triggerRotateSaltPassword(StackDto stack, RotateSaltPasswordReason reason) {
        rotateSaltPasswordValidator.validateRotateSaltPassword(stack);
        RotateSaltPasswordType rotateSaltPasswordType = getRotateSaltPasswordType(stack);
        LOGGER.info("Triggering rotate salt password for stack {} with type {}", stack.getId(), rotateSaltPasswordType);
        return flowManager.triggerRotateSaltPassword(stack.getId(), reason, rotateSaltPasswordType);
    }

    private RotateSaltPasswordType getRotateSaltPasswordType(StackDto stack) {
        return rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack) ? RotateSaltPasswordType.SALT_BOOTSTRAP_ENDPOINT
                : RotateSaltPasswordType.FALLBACK;
    }
}
