package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Objects;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

abstract class AbstractVautEngine<E> implements SecretEngine {

    private Gson gson = new Gson();

    @Override
    public boolean isSecret(String secret) {
        VaultSecret vaultSecret = convertToVaultSecret(secret);
        return vaultSecret != null && vaultSecret.getEngineClass().equals(clazz().getCanonicalName());
    }

    protected VaultSecret convertToVaultSecret(String enginePath, String fullPath) {
        return new VaultSecret(enginePath, clazz().getCanonicalName(), fullPath);
    }

    protected VaultSecret convertToVaultSecret(String secret) {
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
