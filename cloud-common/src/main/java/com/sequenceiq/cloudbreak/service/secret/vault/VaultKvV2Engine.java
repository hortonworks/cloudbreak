package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

@Component("VaultKvV2Engine")
public class VaultKvV2Engine extends AbstractVautEngine<VaultKvV2Engine> {

    @Value("${vault.kv.engine.v2.path:}")
    private String enginePath;

    @Value("#{'${secret.application:}/'}")
    private String appPath;

    @Inject
    private VaultTemplate template;

    @Override
    public String put(String path, String value) {
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        template.opsForVersionedKeyValue(enginePath).put(secret.getPath(), Collections.singletonMap("secret", value));
        return gson().toJson(secret);
    }

    @Override
    public boolean isExists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
    }

    @Override
    @Cacheable(cacheNames = "vaultCache")
    public String get(@NotNull String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null ? String.valueOf(response.getData().get("secret")) : null;
        }).orElse(null);
    }

    @Override
    @CacheEvict(cacheNames = "vaultCache", allEntries = true)
    public void delete(String secret) {
        Optional.ofNullable(convertToVaultSecret(secret)).ifPresent(s -> template.opsForVersionedKeyValue(s.getEnginePath()).delete(s.getPath()));
    }

    @Override
    protected Class<VaultKvV2Engine> clazz() {
        return VaultKvV2Engine.class;
    }
}
