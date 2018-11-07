package com.sequenceiq.cloudbreak.service;

import java.security.InvalidKeyException;
import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

@Service
public class VaultService {

    public static final String SECRET_PATH = "secret";

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultService.class);

    @Inject
    private VaultTemplate template;

    @Value("${cb.vault.kv.path:}")
    private String kvPath;

    /**
     * Stores a secret in Vault's key-value store.
     *
     * @param path  Path where the secret will be stored
     * @param value Secret content
     * @throws Exception is thrown in case the key-value path is already contains a secret
     */
    public void addFieldToSecret(String path, String value) throws Exception {
        String fullPath = kvPath + '/' + path;
        long start = System.currentTimeMillis();
        VaultResponse response = template.read(fullPath);
        LOGGER.debug("Vault read took {} ms", System.currentTimeMillis() - start);
        if (response != null && response.getData() != null) {
            throw new InvalidKeyException(String.format("Path: %s already exists!", fullPath));
        }
        start = System.currentTimeMillis();
        template.write(fullPath, Collections.singletonMap("secret", value));
        LOGGER.debug("Vault write took {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Fetches the secret from Vault's key-value store. If the secret is not found then null is returned.
     * If the path is null then null is returned.
     *
     * @param path Key-value path in Vault
     * @return Secret content or null if the secret path is not found.
     */
    @Cacheable(cacheNames = "vaultCache")
    public String resolveSingleValue(String path) {
        if (path == null) {
            return null;
        } else if (!isSecretPath(path)) {
            return path;
        }
        long start = System.currentTimeMillis();
        VaultResponse response = template.read(path);
        LOGGER.debug("Vault read took {} ms", System.currentTimeMillis() - start);
        if (response != null && response.getData() != null) {
            return String.valueOf(response.getData().get("secret"));
        }
        return null;
    }

    /**
     * Deletes a secret from Vault's key-value store.
     *
     * @param path Key-value path in Vault
     */
    public void deleteSecret(String path) {
        long start = System.currentTimeMillis();
        template.delete(path);
        LOGGER.debug("Vault delete took {} ms", System.currentTimeMillis() - start);
    }

    public boolean isSecretPath(String value) {
        return value.startsWith(kvPath);
    }
}
