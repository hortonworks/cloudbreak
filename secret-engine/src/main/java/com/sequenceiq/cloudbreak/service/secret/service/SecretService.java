package com.sequenceiq.cloudbreak.service.secret.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.service.secret.SecretEngine;
import com.sequenceiq.cloudbreak.service.secret.conf.VaultConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@Service
@ConditionalOnBean({VaultKvV2Engine.class, VaultConfig.class})
public class SecretService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretService.class);

    private final SecretEngine persistentEngine;

    private final VaultRetryService vaultRetryService;

    private final VaultSecretConverter vaultSecretConverter;

    public SecretService(SecretEngine persistentEngine, VaultRetryService vaultRetryService, VaultSecretConverter vaultSecretConverter) {
        this.persistentEngine = persistentEngine;
        this.vaultRetryService = vaultRetryService;
        this.vaultSecretConverter = vaultSecretConverter;
    }

    public String put(String secretPath, String value) throws Exception {
        return vaultRetryService.tryWritingVault(() -> persistentEngine.put(fullSecretPath(secretPath),
                Collections.singletonMap(VaultConstants.FIELD_SECRET, value)));
    }

    public String get(String vaultSecretJson) {
        String field =  ThreadBasedVaultReadFieldProvider.getFieldName(vaultSecretJson);

        if (!isSecret(vaultSecretJson) || field == null) {
            return null;
        }
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        Map<String, String> response = vaultRetryService.tryReadingVault(() -> persistentEngine.getWithCache(vaultSecret.getPath()));
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

    public void deleteByVaultSecretJson(String vaultSecretJson) {
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        if (vaultSecret != null && vaultSecret.getPath() != null) {
            persistentEngine.delete(vaultSecret.getPath());
        }
    }

    public void deleteByPathPostfix(String pathPostfix) {
        if (pathPostfix != null) {
            persistentEngine.delete(persistentEngine.appPath() + pathPostfix);
        }
    }

    public List<String> listEntriesWithoutAppPath(String secretPathPostfix) {
        return persistentEngine.listEntries(persistentEngine.appPath() + secretPathPostfix);
    }

    public boolean isSecret(String secret) {
        return persistentEngine.isSecret(secret);
    }

    public SecretResponse convertToExternal(String secret) {
        return Optional.ofNullable(vaultSecretConverter.convert(secret))
                .map(s -> new SecretResponse(s.getEnginePath(), s.getPath(), s.getVersion()))
                .orElse(null);
    }

    public String getSecretFromExternalVault(String externalVaultPath) {
        Map<String, String> response = vaultRetryService.tryReadingVault(() ->
                persistentEngine.getWithCache(externalVaultPath));
        return response != null ? response.get(VaultConstants.FIELD_SECRET) : null;
    }

    private String fullSecretPath(String secretPath) {
        return persistentEngine.appPath() + secretPath;
    }

    public void cacheEvict(String vaultSecretJson) {
        VaultSecret vaultSecret = vaultSecretConverter.convert(vaultSecretJson);
        persistentEngine.cacheEvict(vaultSecret.getPath());
    }

}