package com.sequenceiq.cloudbreak.rotation.service.notification;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

@Component
public class SecretRotationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationNotificationService.class);

    private static final Set<RotationFlowExecutionType> NOTIFIABLE_EXECUTION_TYPES =
            Set.of(RotationFlowExecutionType.ROTATE, RotationFlowExecutionType.ROLLBACK, RotationFlowExecutionType.FINALIZE);

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public void sendNotification(String resourceCrn, SecretType secretType, SecretRotationStep step, RotationFlowExecutionType executionType) {
        createNotificationMessage(secretType, step, executionType).ifPresent(message -> send(resourceCrn, message));
    }

    private Optional<String> createNotificationMessage(SecretType secretType, SecretRotationStep step, RotationFlowExecutionType executionType) {
        if (step.skipNotification() || !NOTIFIABLE_EXECUTION_TYPES.contains(executionType)) {
            return Optional.empty();
        }
        return Optional.of(executionType.getDisplayName() + "(" + getMessage(secretType) + ") " + getMessage(step));
    }

    protected void send(String resourceCrn, String message) {
    }

    private String getMessage(Object value) {
        String code = value.getClass().getSimpleName() + "." + ((Enum<?>) value).name();
        try {
            return cloudbreakMessagesService.getMessage(code);
        } catch (Exception e) {
            LOGGER.error("Failed to get message for property: {}", code, e);
            return ((Enum<?>) value).name();
        }
    }
}
