package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

abstract class AbstractVaultEngine<E> implements SecretEngine {

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

    protected VaultSecret convertToVaultSecret(String enginePath, String fullPath) {
        return new VaultSecret(enginePath, clazz().getCanonicalName(), fullPath);
    }

    protected VaultSecret convertToVaultSecret(String secret) {
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
