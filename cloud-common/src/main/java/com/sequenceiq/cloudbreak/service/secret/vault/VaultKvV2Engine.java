package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import com.sequenceiq.cloudbreak.service.secret.SecretEngine;

@Component("VaultKvV2Engine")
public class VaultKvV2Engine implements SecretEngine {

    @Value("#{'${cb.secret.application:}/'}")
    private String appPath;

    @Value("${vault.kv.engine.path:}")
    private String enginePath;

    @Inject
    private VaultTemplate template;

    @Override
    public String put(String path, String value) {
        String fullPath = appPath + path;
        template.opsForVersionedKeyValue(enginePath).put(fullPath, Collections.singletonMap("secret", value));
        return fullPath;
    }

    @Override
    public boolean isExists(String path) {
        if (!isSecret(path)) {
            path = appPath + path;
        }
        Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(enginePath).get(path);
        return response != null && response.getData() != null;
    }

    @Override
    @Cacheable(cacheNames = "vaultCache")
    public String get(@NotNull String path) {
        if (!isSecret(path)) {
            return path;
        }
        Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(enginePath).get(path);
        if (response != null && response.getData() != null) {
            return String.valueOf(response.getData().get("secret"));
        }
        return null;
    }

    @Override
    @CacheEvict(cacheNames = "vaultCache", allEntries = true)
    public void delete(String path) {
        template.opsForVersionedKeyValue(enginePath).delete(path);
    }

    @Override
    public boolean isSecret(String value) {
        return value.startsWith(appPath);
    }
}
