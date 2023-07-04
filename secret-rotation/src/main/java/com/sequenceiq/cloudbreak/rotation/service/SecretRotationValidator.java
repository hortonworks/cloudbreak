package com.sequenceiq.cloudbreak.rotation.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SecretType;

@Service
public class SecretRotationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationValidator.class);

    public <T extends Enum<T> & SecretType> List<SecretType> mapSecretTypes(List<String> secrets, Class<T> secretTypeEnum) {
        validateSecrets(secrets);
        try {
            Class<T> enumClass = (Class<T>) Class.forName(secretTypeEnum.getName());
            return secrets.stream()
                    .map(secret -> Enum.valueOf(enumClass, secret))
                    .map(secret -> SecretType.class.cast(secret))
                    .toList();
        } catch (Exception e) {
            String message = String.format("Invalid secret type, cannot map secrets %s to %s", secrets, secretTypeEnum.getSimpleName());
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private void validateSecrets(List<String> secrets) {
        if (secrets.stream().anyMatch(secret -> Collections.frequency(secrets, secret) > 1)) {
            throw new CloudbreakServiceException("There is at least one duplication in the request!");
        }
    }
}
