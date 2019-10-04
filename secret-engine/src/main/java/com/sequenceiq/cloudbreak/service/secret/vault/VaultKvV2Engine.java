package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@Component("VaultKvV2Engine")
@ConditionalOnBean(VaultConfig.class)
public class VaultKvV2Engine extends AbstractVaultEngine<VaultKvV2Engine> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultKvV2Engine.class);

    @Value("${vault.kv.engine.v2.path:}")
    private String enginePath;

    @Value("#{'${secret.application:}/'}")
    private String appPath;

    private VaultTemplate template;

    public VaultKvV2Engine(VaultTemplate template) {
        this.template = template;
    }

    @Override
    public String put(String path, String value) {
        LOGGER.info("Storing secret to {}", path);
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
        Optional.ofNullable(convertToVaultSecret(secret)).ifPresent(s -> deleteAllVersionsOfSecret(s.getEnginePath(), s.getPath()));
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret))
                .map(s -> new SecretResponse(s.getEnginePath(), s.getPath()))
                .orElse(null);
    }

    @Override
    protected Class<VaultKvV2Engine> clazz() {
        return VaultKvV2Engine.class;
    }

    public List<String> listEntries(String path) {
        return template.opsForVersionedKeyValue(enginePath).list(appPath + path);
    }

    @Override
    @CacheEvict(cacheNames = "vaultCache", allEntries = true)
    public void cleanup(String path) {
        deleteAllVersionsOfSecret(enginePath, appPath + path);
    }

    private void deleteAllVersionsOfSecret(String engingPath, String path) {
        template.doWithSession(restOperations -> {
            restOperations.delete("/" + enginePath + "/metadata/" + path);
            return null;
        });
    }
}
