package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

/**
 * Service for rotating secrets, this uses uncached methods and directly interacts with the Vault.Eevery vault interaction
 * is evicting the cache.
 *
 * The reason why this service existis is that during rotation the secrets are overwritten which makes the cache unusable.
 */
@Service
@ConditionalOnBean({VaultKvV2Engine.class, VaultConfig.class})
public class UncachedSecretServiceForRotation {

    private static final Logger LOGGER = LoggerFactory.getLogger(UncachedSecretServiceForRotation.class);

    private final SecretEngine persistentEngine;

    private final VaultRetryService vaultRetryService;

    private final VaultSecretConverter vaultSecretConverter;

    public UncachedSecretServiceForRotation(SecretEngine persistentEngine, VaultRetryService vaultRetryService, VaultSecretConverter vaultSecretConverter) {
        this.persistentEngine = persistentEngine;
        this.vaultRetryService = vaultRetryService;
        this.vaultSecretConverter = vaultSecretConverter;
    }

    public String putRotation(String vaultSecretJson, String newValue) {
        String oldSecretRaw = get(vaultSecretJson);
        return updateRotation(vaultSecretJson, oldSecretRaw, newValue);
    }

    public String update(String vaultSecretJson, String newValue) {
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        String result =  vaultRetryService.tryWritingVault(() -> persistentEngine.put(vaultSecret.getPath(), vaultSecret.getVersion(),
                Map.of(VaultConstants.FIELD_SECRET, newValue)));
        LOGGER.info("Secret on path {} have been updated.", vaultSecret.getPath());
        return result;
    }

    public String updateRotation(String vaultSecretJson, String oldValue, String newValue) {
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        String fullPath = vaultSecret.getPath();
        Integer version = vaultSecret.getVersion();
        Map<String, String> secretValue = Map.of(VaultConstants.FIELD_SECRET, newValue, VaultConstants.FIELD_BACKUP, oldValue);
        String result =  vaultRetryService.tryWritingVault(() -> persistentEngine.put(fullPath, version, secretValue));
        LOGGER.info("Secret on path {} have been updated. ", fullPath);
        return result;
    }

    public RotationSecret getRotation(String vaultSecretJson) {
        if (vaultSecretJson == null) {
            return null;
        }
        String fullPath = vaultSecretConverter.convert(vaultSecretJson).getPath();
        RotationSecret response = vaultRetryService.tryReadingVault(() -> {
            Map<String, String> secretValue = persistentEngine.getWithoutCache(fullPath);
            return secretValue != null ? new RotationSecret(String.valueOf(secretValue.get(VaultConstants.FIELD_SECRET)),
                    String.valueOf(secretValue.get(VaultConstants.FIELD_BACKUP))) : null;
        });
        return response;
    }

    public String get(String vaultSecretJson) {
        String field = ThreadBasedVaultReadFieldProvider.getFieldName(vaultSecretJson);
        if (!isSecret(vaultSecretJson) || field == null) {
            return null;
        }
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        Map<String, String> response = vaultRetryService.tryReadingVault(() -> {
            return persistentEngine.getWithoutCache(vaultSecret.getPath());

        });
        return response != null ? response.get(field) : null;
    }

    public String getBySecretPath(String secretPath, String field) {
        if (secretPath == null || field == null) {
            return null;
        }
        Map<String, String> response = vaultRetryService.tryReadingVault(() -> {
            return persistentEngine.getWithoutCache(secretPath);
        });
        return response != null ? response.get(field) : null;
    }

    private String get(String vaultSecretJson, String field) {
        if (!isSecret(vaultSecretJson) || field == null) {
            return null;
        }
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        Map<String, String> response = vaultRetryService.tryReadingVault(() -> {
            return persistentEngine.getWithoutCache(vaultSecret.getPath());

        });
        return response != null ? response.get(field) : null;
    }

    public String getByResponse(SecretResponse secretResponse) {
        if (secretResponse == null) {
            return null;
        }
        VaultSecret vaultSecret = new VaultSecret(secretResponse.getEnginePath(), VaultKvV2Engine.class.getCanonicalName(),
                secretResponse.getSecretPath(), secretResponse.getSecretVersion());
        String secretAsJson = new Gson().toJson(vaultSecret);
        return get(secretAsJson);
    }

    public boolean isSecret(String secret) {
        return persistentEngine.isSecret(secret);
    }

}
