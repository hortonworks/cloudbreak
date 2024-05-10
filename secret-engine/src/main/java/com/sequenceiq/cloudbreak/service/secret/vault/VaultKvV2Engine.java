package com.sequenceiq.cloudbreak.service.secret.vault;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@Component("VaultKvV2Engine")
@ConditionalOnBean(VaultConfig.class)
public class VaultKvV2Engine implements SecretEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultKvV2Engine.class);

    @Value("${vault.kv.engine.v2.path:}")
    private String enginePath;

    @Value("#{'${secret.application:}/'}")
    private String appPath;

    private final MetricService metricService;

    private final VaultTemplate vaultRestTemplate;

    private final VaultSecretInputValidator vaultSecretInputValidator;

    private final VaultSecretConverter vaultSecretConverter;

    public VaultKvV2Engine(@Qualifier("CommonMetricService") MetricService metricService, VaultTemplate vaultRestTemplate,
            VaultSecretInputValidator vaultSecretInputValidator, VaultSecretConverter vaultSecretConverter) {
        this.metricService = metricService;
        this.vaultRestTemplate = vaultRestTemplate;
        this.vaultSecretInputValidator = vaultSecretInputValidator;
        this.vaultSecretConverter = vaultSecretConverter;
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
    public boolean isSecret(String secret) {
        VaultSecret vaultSecret = vaultSecretConverter.convert(secret);
        return vaultSecret != null && vaultSecret.getEngineClass().equals(getClass().getCanonicalName());
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, String value) {
        return put(path, Collections.singletonMap(VaultConstants.FIELD_SECRET, value));
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public String put(String path, Map<String, String> value) {
        long start = System.currentTimeMillis();
        LOGGER.info("Storing secret to {}", path);
        String fullPath = appPath + path;
        Versioned.Metadata metadata = vaultRestTemplate.opsForVersionedKeyValue(enginePath).put(fullPath, value);
        VaultSecret vaultSecret = new VaultSecret(enginePath, getClass().getCanonicalName(), fullPath);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_WRITE, Duration.ofMillis(duration));
        LOGGER.trace("Secret write took {} ms, version: {}", duration, metadata.getVersion().getVersion());
        return JsonUtil.writeValueAsStringSilent(vaultSecret);
    }

    @Override
    public boolean exists(String secret) {
        long start = System.currentTimeMillis();
        boolean ret = Optional.ofNullable(vaultSecretConverter.convert(secret)).map(s -> {
            Versioned<Map<String, Object>> response = vaultRestTemplate.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null;
        }).orElse(false);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret read took {} ms", duration);
        return ret;
    }

    @Override
    @Cacheable(cacheNames = VaultConstants.CACHE_NAME)
    public String get(@NotNull String secret, @NotNull String field) {
        long start = System.currentTimeMillis();
        String ret = Optional.ofNullable(vaultSecretConverter.convert(secret)).map(s -> {
            Versioned<Map<String, Object>> response = vaultRestTemplate.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            return response != null && response.getData() != null && response.getData().containsKey(field) ?
                    String.valueOf(response.getData().get(field)) : null;
        }).orElse(null);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret read took {} ms", duration);
        return ret;
    }

    @Override
    public RotationSecret getRotation(@NotNull String secret) {
        long start = System.currentTimeMillis();
        RotationSecret rotationSecret = Optional.ofNullable(vaultSecretConverter.convert(secret)).map(s -> {
            Versioned<Map<String, Object>> response = vaultRestTemplate.opsForVersionedKeyValue(s.getEnginePath()).get(s.getPath());
            logRotationMeta(response);
            return response != null && response.getData() != null ?
                    new RotationSecret(String.valueOf(response.getData().get(VaultConstants.FIELD_SECRET)),
                            String.valueOf(response.getData().get(VaultConstants.FIELD_BACKUP))) : null;
        }).orElse(null);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret read took {} ms", duration);
        return rotationSecret;
    }

    private void logRotationMeta(Versioned<Map<String, Object>> response) {
        boolean ongoingRotation = response.getData().get(VaultConstants.FIELD_BACKUP) != null;
        LOGGER.info("Backup value is set: {}. Rotation secret metadata: {}", ongoingRotation, response.getMetadata());
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public void delete(String secret) {
        Optional.ofNullable(vaultSecretConverter.convert(secret)).ifPresent(s -> deleteAllVersionsOfSecret(s.getEnginePath(), s.getPath()));
    }

    @Override
    public SecretResponse convertToExternal(String secret) {
        return Optional.ofNullable(vaultSecretConverter.convert(secret))
                .map(s -> new SecretResponse(s.getEnginePath(), s.getPath()))
                .orElse(null);
    }

    public List<String> listEntries(String path) {
        long start = System.currentTimeMillis();
        List<String> ret = vaultRestTemplate.opsForVersionedKeyValue(enginePath).list(appPath + path);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret list took {} ms", duration);
        return ret;
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, allEntries = true)
    public void cleanup(String path) {
        deleteAllVersionsOfSecret(enginePath, appPath + path);
    }

    private void deleteAllVersionsOfSecret(String engingPath, String path) {
        long start = System.currentTimeMillis();
        vaultRestTemplate.doWithSession(restOperations -> {
            restOperations.delete("/" + enginePath + "/metadata/" + path);
            return null;
        });
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_DELETE, Duration.ofMillis(duration));
        LOGGER.trace("Secret delete took {} ms", duration);
    }
}
