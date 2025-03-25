package com.sequenceiq.cloudbreak.rotation.service.notification;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;

@Component
public class SecretRotationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationNotificationService.class);

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public String getMessage(SerializableRotationEnum rotationEnum, SecretListField secretListField) {
        String code = rotationEnum.getClazz().getSimpleName() + "." + secretListField.name() + "." + rotationEnum.value();
        try {
            return cloudbreakMessagesService.getMessage(code);
        } catch (Exception e) {
            LOGGER.error("Failed to get message for property: {}", code, e);
            return rotationEnum.value();
        }
    }
}
