package com.sequenceiq.cloudbreak.service.secret.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;

@Component
public class VaultSecretInputValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultSecretInputValidator.class);

    @Value("${vault.kv.engine.max.secret.path.length:255}")
    private int maxSecretPathLength;

    public void validate(String enginePath, String fullPath) {
        LOGGER.info("Converting secret enginePath: {}, fullPath: {}", enginePath, fullPath);

        if (Strings.isNullOrEmpty(enginePath) || Strings.isNullOrEmpty(fullPath)) {
            throw new SecretOperationException(String.format("EnginePath and and secretPath cannot be null or " +
                    "enginePath:[%s], fullPath [%s]", enginePath, fullPath));
        }
        // + 1 is because of the delimiter
        int secretPathLength = enginePath.length() + fullPath.length() + 1;

        if (secretPathLength > maxSecretPathLength) {
            throw new SecretOperationException(String.format("Secret path size [%s] is greater than [%s]", secretPathLength, maxSecretPathLength));
        }
    }
}
