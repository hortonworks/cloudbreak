package com.sequenceiq.secret.service;

import static java.lang.String.format;

import java.security.InvalidKeyException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.secret.SecretEngine;
import com.sequenceiq.secret.model.SecretResponse;

@Service
public class SecretService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretService.class);

    @Value("${secret.engine:}")
    private String engineClass;

    @Inject
    private MetricService metricService;

    @Inject
    private List<SecretEngine> engines;

    private SecretEngine persistentEngine;

//    public SecretService(MetricService metricService, List<SecretEngine> engines) {
//        this.metricService = metricService;
//        this.engines = engines;
//    }

    @PostConstruct
    public void init() {
        if (StringUtils.hasLength(engineClass)) {
            persistentEngine = engines.stream().filter(e -> e.getClass().getCanonicalName().startsWith(engineClass + "$$")).findFirst()
                    .orElseThrow(() -> new RuntimeException(format("Selected secret engine (%s) is not found, please check cb.secret.engine", engineClass)));
        }
    }

    /**
     * Stores a secret in Secret's key-value store.
     *
     * @param key   Path where the secret will be stored
     * @param value Secret content
     * @throws Exception is thrown in case the key-value key is already contains a secret
     */
    public String put(String key, String value) throws Exception {
        long start = System.currentTimeMillis();
        boolean exists = persistentEngine.isExists(key);
        long duration = System.currentTimeMillis() - start;
        metricService.submit(MetricType.VAULT_READ, duration);
        LOGGER.debug("Secret read took {} ms", duration);
        if (exists) {
            throw new InvalidKeyException(format("Key: %s already exists!", key));
        }
        start = System.currentTimeMillis();
        String secret = persistentEngine.put(key, value);
        duration = System.currentTimeMillis() - start;
        metricService.submit(MetricType.VAULT_WRITE, duration);
        LOGGER.debug("Secret write took {} ms", duration);
        metricService.incrementMetricCounter(() -> "secret.write." + convertSecretToMetric(secret));
        return secret;
    }

    /**
     * Fetches the secret from Secret's store. If the secret is not found then null is returned.
     * If the secret is null then null is returned.
     *
     * @param secret Key-value secret in Secret
     * @return Secret content or null if the secret secret is not found.
     */
    public String get(String secret) {
        if (secret == null) {
            return null;
        }
        metricService.incrementMetricCounter(() -> "secret.read." + convertSecretToMetric(secret));
        long start = System.currentTimeMillis();
        String response = getFirstEngineStream(secret)
                .map(e -> e.get(secret))
                .filter(Objects::nonNull)
                .orElse(null);
        long duration = System.currentTimeMillis() - start;
        metricService.submit(MetricType.VAULT_READ, duration);
        LOGGER.debug("Secret read took {} ms", duration);
        return "null".equals(response) ? null : response;
    }

    private Optional<SecretEngine> getFirstEngineStream(String secret) {
        return engines.stream()
                .filter(e -> e.isSecret(secret))
                .findFirst();
    }

    /**
     * Deletes a secret from Secrets's store.
     *
     * @param secret Key-value secret in Secret
     */
    public void delete(String secret) {
        metricService.incrementMetricCounter(() -> "secret.delete." + convertSecretToMetric(secret));
        long start = System.currentTimeMillis();
        engines.stream()
                .filter(e -> e.isSecret(secret))
                .forEach(e -> e.delete(secret));
        long duration = System.currentTimeMillis() - start;
        metricService.submit(MetricType.VAULT_WRITE, duration);
        LOGGER.debug("Secret delete took {} ms", duration);
    }

    /**
     * Determines the secret is a secret location or not
     *
     * @param secret Key-value key in Secret
     */
    public boolean isSecret(String secret) {
        return engines.stream().anyMatch(e -> e.isSecret(secret));
    }

    /**
     * Converts secret to external for API
     *
     * @param secret internal secret
     * @return public external secret JSON
     */
    public SecretResponse convertToExternal(String secret) {
        return getFirstEngineStream(secret)
                .map(e -> e.convertToExternal(secret))
                .orElse(null);
    }

    private String convertSecretToMetric(String secret) {
        return getFirstEngineStream(secret)
                .map(e -> e.scarifySecret(secret))
                .orElse(null);
    }
}
