package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@Service
@ConditionalOnBean({VaultKvV2Engine.class, VaultConfig.class})
public class SecretService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretService.class);

    @Value("${secret.engine:}")
    private String engineClass;

    private final SecretEngine persistentEngine;

    private final VaultRetryService vaultRetryService;

    public SecretService(SecretEngine persistentEngine, VaultRetryService vaultRetryService) {
        this.persistentEngine = persistentEngine;
        this.vaultRetryService = vaultRetryService;
    }

    /**
     * Stores a secret in Secret's key-value store.
     *
     * @param key   Path where the secret will be stored
     * @param value Secret content
     * @throws Exception is thrown in case the key-value key is already contains a secret
     */
    public String put(String key, String value) throws Exception {
        return vaultRetryService.tryWritingVault(() -> persistentEngine.put(key, value));
    }

    public String put(String key, Map<String, String> value) throws Exception {
        return vaultRetryService.tryWritingVault(() -> persistentEngine.put(key, value));
    }

    public String putRotation(String secret, String newValue) throws Exception {
        String oldSecretRaw = get(secret);
        return updateRotation(secret, oldSecretRaw, newValue);
    }

    public String update(String secret, String newValue) throws Exception {
        String fullPath = convertToExternal(secret).getSecretPath();
        String result = put(fullPath.split(persistentEngine.appPath(), 2)[1], newValue);
        LOGGER.info("Secret on path {} have been updated.", fullPath);
        return result;
    }

    public String updateRotation(String secret, String oldValue, String newValue) throws Exception {
        String fullPath = convertToExternal(secret).getSecretPath();
        String result = put(fullPath.split(persistentEngine.appPath(), 2)[1],
                Map.of(VaultConstants.FIELD_SECRET, newValue, VaultConstants.FIELD_BACKUP, oldValue));
        LOGGER.info("Secret on path {} have been updated.", fullPath);
        return result;
    }

    public RotationSecret getRotation(String secret) {
        if (secret == null) {
            return null;
        }

        RotationSecret response = vaultRetryService.tryReadingVault(() -> {
            return persistentEngine.getRotation(secret);
        });
        return "null".equals(response) ? null : response;
    }

    /**
     * Fetches the secret from Secret's store. If the secret is not found then null is returned.
     * If the secret is null then null is returned.
     *
     * @param secret Key-value secret in Secret
     * @return Secret content or null if the secret secret is not found.
     */
    public String get(String secret) {
        return get(secret, ThreadBasedVaultReadFieldProvider.getFieldName(secret));
    }

    /**
     * Fetches the field of the secret from Secret's store. If the secret is not found then null is returned.
     * If the secret is null then null is returned.
     *
     * @param secret Key-value secret in Secret
     * @param field  The key of the key-value secret in Secret
     * @return Secret content or null if the secret is not found.
     */
    private String get(String secret, String field) {
        if (!isSecret(secret) || field == null) {
            return null;
        }
        String response = vaultRetryService.tryReadingVault(() -> {
            return persistentEngine.get(secret, field);
        });
        return "null".equals(response) ? null : response;
    }

    /**
     * Fetches the secret from Secret's store. If the secret is not found then null is returned.
     * If the secret is null then null is returned.
     *
     * @param secretResponse SecretResponse that refers to a Secret in the Secret engine
     * @return Secret content or null if the secret secret is not found.
     */
    public String getByResponse(SecretResponse secretResponse) {
        if (secretResponse == null) {
            return null;
        }
        VaultSecret vaultSecret = new VaultSecret(secretResponse.getEnginePath(), VaultKvV2Engine.class.getCanonicalName(),
                secretResponse.getSecretPath(), secretResponse.getSecretVersion());
        String secretAsJson = new Gson().toJson(vaultSecret);
        return get(secretAsJson);
    }

    /**
     * Fetches the secret from Secret's store. If the secret is not found then null is returned.
     * If the secret is null then null is returned.
     *
     * @param secretPath path of the Secret that refers to a Secret in the Secret engine
     * @param field      The key of the key-value secret in Secret
     * @return Secret content or null if the secret is not found.
     */
    public String getLatestSecretWithoutCache(String secretPath, String field) {
        if (secretPath == null || field == null) {
            return null;
        }
        String response = vaultRetryService.tryReadingVault(() -> persistentEngine.getLatestSecretWithoutCache(secretPath, field));
        return "null".equals(response) ? null : response;
    }

    /**
     * Deletes a secret from Secrets's store.
     *
     * @param secret Key-value secret in Secret
     */
    public void delete(String secret) {
        if (isSecret(secret)) {
            persistentEngine.delete(secret);
        }
    }

    public List<String> listEntries(String secretPathPrefix) {
        return persistentEngine.listEntries(secretPathPrefix);
    }

    public void cleanup(String pathPrefix) {
        if (pathPrefix != null) {
            persistentEngine.cleanup(pathPrefix);
        }
    }

    /**
     * Determines the secret is a secret location or not
     *
     * @param secret Key-value key in Secret
     */
    public boolean isSecret(String secret) {
        return persistentEngine.isSecret(secret);
    }

    /**
     * Converts secret to external for API
     *
     * @param secret internal secret
     * @return public external secret JSON
     */
    public SecretResponse convertToExternal(String secret) {
        return persistentEngine.convertToExternal(secret);
    }
}
