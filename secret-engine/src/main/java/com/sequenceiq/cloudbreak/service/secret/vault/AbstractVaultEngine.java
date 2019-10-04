package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;

abstract class AbstractVaultEngine<E> implements SecretEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVaultEngine.class);

    @Value("${vault.kv.engine.max.secret.path.length:255}")
    private int maxSecretPathLength;

    private final Gson gson = new Gson();

    private final Pattern uniqueId = Pattern.compile("(.*)/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}-[a-fA-F0-9]{3,20}");

    @Override
    public boolean isSecret(String secret) {
        VaultSecret vaultSecret = convertToVaultSecret(secret);
        return vaultSecret != null && vaultSecret.getEngineClass().equals(clazz().getCanonicalName());
    }

    @Override
    public String scarifySecret(String secret) {
        VaultSecret vaultSecret = convertToVaultSecret(secret);
        int cut = uniqueId.matcher(vaultSecret.getPath()).matches() ? 1 : 0;
        return Optional.of(vaultSecret)
                .map(s -> s.getPath().split("/"))
                .map(ss -> Stream.of(ss).limit(ss.length - cut))
                .map(st -> st.collect(Collectors.joining(".")))
                .orElse(null);
    }

    VaultSecret convertToVaultSecret(String enginePath, String fullPath) {
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

        return new VaultSecret(enginePath, clazz().getCanonicalName(), fullPath);
    }

    VaultSecret convertToVaultSecret(String secret) {
        if (secret == null) {
            return null;
        }
        try {
            VaultSecret vaultSecret = gson.fromJson(secret, VaultSecret.class);
            if (Stream.of(vaultSecret.getEnginePath(), vaultSecret.getEngineClass(), vaultSecret.getPath()).allMatch(Objects::nonNull)) {
                return vaultSecret;
            }
        } catch (JsonSyntaxException ignore) {
        }
        return null;
    }

    protected Gson gson() {
        return gson;
    }

    protected abstract Class<E> clazz();
}
