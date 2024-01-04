package com.sequenceiq.cloudbreak.rotation.service.notification;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@Component
public class SecretRotationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationNotificationService.class);

    private static final Set<RotationFlowExecutionType> NOTIFIABLE_EXECUTION_TYPES =
            Set.of(RotationFlowExecutionType.ROTATE, RotationFlowExecutionType.ROLLBACK, RotationFlowExecutionType.FINALIZE);

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public void sendNotification(RotationMetadata metadata, SecretRotationStep step) {
        createNotificationMessage(metadata.secretType(), step, metadata.currentExecution()).ifPresent(message -> send(metadata.resourceCrn(), message));
    }

    public String getMessage(SerializableRotationEnum rotationEnum) {
        String code = rotationEnum.getClazz().getSimpleName() + "." + rotationEnum.value();
        try {
            return cloudbreakMessagesService.getMessage(code);
        } catch (Exception e) {
            LOGGER.error("Failed to get message for property: {}", code, e);
            return rotationEnum.value();
        }
    }

    private Optional<String> createNotificationMessage(SecretType secretType, SecretRotationStep step, RotationFlowExecutionType executionType) {
        if (step.skipNotification() || !NOTIFIABLE_EXECUTION_TYPES.contains(executionType)) {
            return Optional.empty();
        }
        return Optional.of(getMessage(executionType) + " secret [" + getMessage(secretType) + "]: " + getMessage(step));
    }

    protected void send(String resourceCrn, String message) {
    }
}
