package com.sequenceiq.cloudbreak.service.secret.vault;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@Component("VaultKvV2Engine")
@ConditionalOnBean(VaultConfig.class)
public class VaultKvV2Engine implements SecretEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultKvV2Engine.class);

    private static final String PATH_PATTERN = "^[^{}]*$";

    private final MetricService metricService;

    private final VaultTemplate vaultRestTemplate;

    private final VaultSecretConverter vaultSecretConverter;

    @Value("${vault.kv.engine.v2.path:}")
    private String enginePath;

    @Value("#{'${secret.application:}/'}")
    private String appPath;

    public VaultKvV2Engine(@Qualifier("CommonMetricService") MetricService metricService, VaultTemplate vaultRestTemplate,
            VaultSecretConverter vaultSecretConverter) {
        this.metricService = metricService;
        this.vaultRestTemplate = vaultRestTemplate;
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
    public boolean isSecret(String vaultSecretJson) {
        if (vaultSecretJson == null) {
            return false;
        }
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        return vaultSecret != null && vaultSecret.getEngineClass().equals(getClass().getCanonicalName());
    }

    @Override
    @Cacheable(cacheNames = VaultConstants.CACHE_NAME, key = "{#fullSecretPath, #version}")
    public Map<String, String> getWithCache(String fullSecretPath, Integer version) {
        return get(fullSecretPath, version);
    }

    @Override
    public Map<String, String> getWithoutCache(String fullSecretPath) {
        return get(fullSecretPath, null);
    }

    private Map<String, String> get(@NotNull String fullSecretPath, @NotNull Integer version) {
        validatePathPattern(fullSecretPath);
        long start = System.currentTimeMillis();
        Map<String, String> ret = null;
        Versioned<Map<String, Object>> response;
        if (version != null) {
            response = vaultRestTemplate.opsForVersionedKeyValue(enginePath).get(fullSecretPath, Versioned.Version.from(version));
        } else {
            response = vaultRestTemplate.opsForVersionedKeyValue(enginePath).get(fullSecretPath);
        }
        if (response != null && response.getData() != null) {
            ret = new HashMap<>();
            for (String field : response.getData().keySet()) {
                ret.put(field, String.valueOf(response.getData().get(field)));
            }
        }
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret read took {} ms", duration);
        return ret;
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, key = "{#fullSecretPath, #currentVersion}")
    public String put(String fullSecretPath, Integer currentVersion, Map<String, String> value) {
        validatePathOwnedByApp(fullSecretPath, "store");
        long start = System.currentTimeMillis();
        LOGGER.info("Storing secret to {}", fullSecretPath);
        Versioned.Metadata metadata = vaultRestTemplate.opsForVersionedKeyValue(enginePath).put(fullSecretPath, value);
        Integer version = Optional.ofNullable(metadata)
                .map(Versioned.Metadata::getVersion)
                .map(Versioned.Version::getVersion)
                .orElse(null);
        VaultSecret vaultSecret = new VaultSecret(enginePath, getClass().getCanonicalName(), fullSecretPath, version);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_WRITE, Duration.ofMillis(duration));
        LOGGER.trace("Secret write took {} ms, version: {}", duration, version);
        return JsonUtil.writeValueAsStringSilent(vaultSecret);
    }

    @Override
    public List<String> listEntries(String fullSecretPath) {
        validatePathPattern(fullSecretPath);
        long start = System.currentTimeMillis();
        List<String> ret = vaultRestTemplate.opsForVersionedKeyValue(enginePath).list(fullSecretPath);
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_READ, Duration.ofMillis(duration));
        LOGGER.trace("Secret list took {} ms", duration);
        return ret;
    }

    @Override
    @CacheEvict(cacheNames = VaultConstants.CACHE_NAME, key = "{#fullSecretPath, #version}")
    public void delete(String fullSecretPath, Integer version) {
        validatePathOwnedByApp(fullSecretPath, "delete");
        deleteAllVersionsOfSecret(fullSecretPath);
    }

    private void deleteAllVersionsOfSecret(String secretPath) {
        long start = System.currentTimeMillis();
        vaultRestTemplate.doWithSession(restOperations -> {
            restOperations.delete("/" + enginePath + "/metadata/" + secretPath);
            return null;
        });
        long duration = System.currentTimeMillis() - start;
        metricService.recordTimerMetric(MetricType.VAULT_DELETE, Duration.ofMillis(duration));
        LOGGER.trace("Secret delete took {} ms", duration);
    }

    private void validatePathPattern(String secretPath) {
        String errorMessage = null;
        if (StringUtils.isEmpty(secretPath)) {
            errorMessage = String.format("Secret path cannot be null or empty: %s", secretPath);
        } else if (!secretPath.matches(PATH_PATTERN)) {
            errorMessage = String.format("Path contains invalid characters: %s", secretPath);
        } else if (secretPath.indexOf(appPath, appPath.length()) > 0) {
            errorMessage = String.format("App path occurs multiple times. App: '%s', secretPath: '%s'", appPath, secretPath);
        }

        if (errorMessage != null) {
            VaultIllegalArgumentException exc = new VaultIllegalArgumentException(errorMessage);
            LOGGER.error("Invalid secret path!", exc);
            throw exc;
        }
    }

    private void validatePathOwnedByApp(String secretPath, String command) {
        validatePathPattern(secretPath);
        if (!secretPath.startsWith(appPath)) {
            VaultIllegalArgumentException exc = new VaultIllegalArgumentException(String.format("Can't %s secret, if the secret is not owned by the app. " +
                    "App: '%s', secretPath: '%s'", command, appPath, secretPath));
            LOGGER.error("Invalid secret path!", exc);
            throw exc;
        }
    }
}
