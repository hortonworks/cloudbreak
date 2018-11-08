package com.sequenceiq.cloudbreak.service.secret;

import java.security.InvalidKeyException;
import java.util.List;

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

    private SecretEngine secretEngine;

    @PostConstruct
    public void init() {
        if (StringUtils.hasLength(engineClass)) {
            secretEngine = engines.stream().filter(e -> e.getClass().getCanonicalName().startsWith(engineClass + "$$")).findFirst()
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
        boolean exists = secretEngine.isExists(key);
        LOGGER.debug("Secret read took {} ms", System.currentTimeMillis() - start);
        if (exists) {
            throw new InvalidKeyException(String.format("Path: %s already exists!", key));
        }
        start = System.currentTimeMillis();
        String finalPath = secretEngine.put(key, value);
        LOGGER.debug("Secret write took {} ms", System.currentTimeMillis() - start);
        return finalPath;
    }

    /**
     * Fetches the secret from Secret's store. If the secret is not found then null is returned.
     * If the key is null then null is returned.
     *
     * @param key Key-value key in Secret
     * @return Secret content or null if the secret key is not found.
     */
    public String get(String key) {
        if (key == null) {
            return null;
        }
        long start = System.currentTimeMillis();
        String response = secretEngine.get(key);
        LOGGER.debug("Secret read took {} ms", System.currentTimeMillis() - start);
        return response;
    }

    /**
     * Deletes a secret from Secrets's store.
     *
     * @param key Key-value key in Secret
     */
    public void delete(String key) {
        long start = System.currentTimeMillis();
        secretEngine.delete(key);
        LOGGER.debug("Secret delete took {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Determines the value is a secret location or not
     *
     * @param value Key-value key in Secret
     */
    public boolean isSecret(String value) {
        return secretEngine.isSecret(value);
    }
}
