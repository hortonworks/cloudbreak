package com.sequenceiq.cloudbreak.service.secret.vault;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

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
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

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
    public String appPath() {
        return appPath;
    }

    @Override
    public String enginePath() {
        return enginePath;
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, String value) {
        return put(path, Collections.singletonMap(VaultConstants.FIELD_SECRET, value));
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, Map<String, String> value) {
        LOGGER.info("Storing secret to {}", path);
        VaultSecret secret = convertToVaultSecret(enginePath, appPath + path);
        template.opsForVersionedKeyValue(enginePath).put(secret.getPath(), value);
        return gson().toJson(secret);
    }

    @Override
    public boolean exists(String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
    }

    @Override
    @Cacheable(cacheNames = VaultConstants.CACHE_NAME)
    public String get(@NotNull String secret, @NotNull String field) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null && response.getData().containsKey(field) ?
                    String.valueOf(response.getData().get(field)) : null;
        }).orElse(null);
    }

    @Override
    public RotationSecret getRotation(@NotNull String secret) {
        return Optional.ofNullable(convertToVaultSecret(secret)).map(s -> {
            Versioned<Map<String, Object>> response = template.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            logRotationMeta(response);
            return response != null && response.getData() != null ?
                    new RotationSecret(String.valueOf(response.getData().get(VaultConstants.FIELD_SECRET)),
                            String.valueOf(response.getData().get(VaultConstants.FIELD_BACKUP))) : null;
        }).orElse(null);
    }

    private void logRotationMeta(Versioned<Map<String, Object>> response) {
        boolean ongoingRotation = response.getData().get(VaultConstants.FIELD_BACKUP) != null;
        LOGGER.info("Backup value is set: {}. Rotation secret metadata: {}", ongoingRotation, response.getMetadata());
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
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
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
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
