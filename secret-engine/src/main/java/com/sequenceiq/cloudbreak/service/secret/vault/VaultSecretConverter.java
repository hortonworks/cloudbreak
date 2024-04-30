package com.sequenceiq.cloudbreak.service.secret.vault;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class VaultSecretConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultSecretConverter.class);

    public VaultSecret convert(String secret) {
        if (secret == null) {
            return null;
        }
        try {
            VaultSecret vaultSecret = JsonUtil.readValue(secret, VaultSecret.class);
            if (Stream.of(vaultSecret.getEnginePath(), vaultSecret.getEngineClass(), vaultSecret.getPath()).allMatch(Objects::nonNull)) {
                return vaultSecret;
            }
        } catch (IOException ignore) {
            LOGGER.warn("Parsing vault secret failed, ignore exception", ignore);
        }
        return null;
    }
}
