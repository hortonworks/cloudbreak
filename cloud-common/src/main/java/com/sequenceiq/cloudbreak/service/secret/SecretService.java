package com.sequenceiq.cloudbreak.service.secret;

import java.security.InvalidKeyException;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SecretService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretService.class);

    @Value("${secret.engine:}")
    private String engineClass;

    @Inject
    private List<SecretEngine> engines;

    private SecretEngine persistentEngine;

    @PostConstruct
    public void init() {
        if (StringUtils.hasLength(engineClass)) {
            persistentEngine = engines.stream().filter(e -> e.getClass().getCanonicalName().startsWith(engineClass + "$$")).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Selected secret engine (%s) is not found, please check cb.secret.engine", engineClass)));
        }
    }

    /**
     * Stores a secret in Secret's key-value store.
     *
     * @param key  Path where the secret will be stored
     * @param value Secret content
     * @throws Exception is thrown in case the key-value key is already contains a secret
     */
    public String put(String key, String value) throws Exception {
        long start = System.currentTimeMillis();
        boolean exists = persistentEngine.isExists(key);
        LOGGER.debug("Secret read took {} ms", System.currentTimeMillis() - start);
        if (exists) {
            throw new InvalidKeyException(String.format("Key: %s already exists!", key));
        }
        start = System.currentTimeMillis();
        String secret = persistentEngine.put(key, value);
        LOGGER.debug("Secret write took {} ms", System.currentTimeMillis() - start);
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
        long start = System.currentTimeMillis();
        String response = engines.stream().filter(e -> e.isSecret(secret)).map(e -> e.get(secret))
                .filter(Objects::nonNull).findFirst().orElse(null);
        LOGGER.debug("Secret read took {} ms", System.currentTimeMillis() - start);
        return "null".equals(response) ? null : response;
    }

    /**
     * Deletes a secret from Secrets's store.
     *
     * @param secret Key-value secret in Secret
     */
    public void delete(String secret) {
        long start = System.currentTimeMillis();
        engines.stream().filter(e -> e.isSecret(secret)).forEach(e -> e.delete(secret));
        LOGGER.debug("Secret delete took {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Determines the secret is a secret location or not
     *
     * @param secret Key-value key in Secret
     */
    public boolean isSecret(String secret) {
        return engines.stream().anyMatch(e -> e.isSecret(secret));
    }
}
