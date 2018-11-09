package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Component("VaultKvV1Engine")
public class VaultKvV1Engine extends AbstractVautEngine<VaultKvV1Engine> {

    @Value("${vault.kv.engine.path:}")
    private String enginePath;

    @Value("#{'${vault.kv.engine.path:}/${secret.application:}/'}")
    private String appPath;

    @Inject
    private VaultTemplate template;

    @Override
    public String put(String path, String value) {
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        template.write(secret.getPath(), Collections.singletonMap("secret", value));
        return gson().toJson(secret);
    }

    @Override
    public boolean isExists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
    }

    @Override
    @Cacheable(cacheNames = "vaultCache")
    public String get(@NotNull String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            VaultResponse response = template.read(s.getPath());
            return response != null && response.getData() != null ? String.valueOf(response.getData().get("secret")) : null;
        }).orElse(null);
    }

    @Override
    @CacheEvict(cacheNames = "vaultCache", allEntries = true)
    public void delete(String secret) {
        Optional.ofNullable(convertToVaultSecret(secret)).ifPresent(s -> template.delete(s.getPath()));
    }

    @Override
    protected Class<VaultKvV1Engine> clazz() {
        return VaultKvV1Engine.class;
    }
}
