package com.sequenceiq.redbeams.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;

import com.sequenceiq.secret.model.SecretResponse;
import com.sequenceiq.secret.vault.VaultSecret;


@Component
public class VaultMockEngine extends com.sequenceiq.secret.vault.VaultKvV2Engine {

    @Value("${vault.kv.engine.v2.path:}")
    private String enginePath;

    @Value("#{'${secret.application:}/'}")
    private String appPath;

    private final Map<String, String> secretStore = new HashMap<>();

    public VaultMockEngine(VaultTemplate template) {
        super(template);
    }

    @Override
    protected Class<com.sequenceiq.secret.vault.VaultKvV2Engine> clazz() {
        return com.sequenceiq.secret.vault.VaultKvV2Engine.class;
    }

    @Override
    public String put(String path, String value) {
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        secretStore.put(secret.getPath(), value);
        return gson().toJson(secret);
    }

    @Override
    public boolean isExists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret))
                .map(s -> secretStore.containsKey(s.getPath()))
                .orElse(false);
    }

    @Override
    public String get(String secret) {
        return secretStore.get(convertToVaultSecret(secret).getPath());
    }

    @Override
    public void delete(String secret) {
        secretStore.remove(convertToVaultSecret(secret).getPath());
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret))
                .map(s -> new SecretResponse(s.getEnginePath(), s.getPath()))
                .orElse(null);
    }
}
