package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;

@Service
public class SyncSecretVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncSecretVersionService.class);

    @Inject
    private VaultSecretConverter vaultSecretConverter;

    @Inject
    private SecretService secretService;

    public void updateEntityIfNeeded(String crn, Object entity, Set<SecretMarker> secretMarkers) {
        if (secretMarkers.stream().anyMatch(marker -> updateSecretFieldIfNeeded(entity, marker))) {
            LOGGER.info("There were update for {} entity of resource {}, thus saving it into database!", entity.getClass(), crn);
            VaultRotationReflectionUtil.saveEntity(entity);
        }
    }

    private boolean updateSecretFieldIfNeeded(Object entity, SecretMarker secretMarker) {
        try {
            Optional<String> secretJsonStringFromEntity = VaultRotationReflectionUtil.getVaultSecretJson(entity, secretMarker);
            if (secretJsonStringFromEntity.isPresent() && StringUtils.isNotEmpty(secretJsonStringFromEntity.get())) {
                VaultSecret secretJsonObjectFromEntity = vaultSecretConverter.convert(secretJsonStringFromEntity.get());
                LOGGER.info("Checking if there is a difference between secret version stored in database ({}) and vault for secret path {}",
                        secretJsonObjectFromEntity.getVersion(), secretJsonObjectFromEntity.getPath());
                Optional<Integer> latestVersionFromVault = secretService.getVersion(secretJsonStringFromEntity.get());
                if (latestVersionFromVault.isPresent() && !latestVersionFromVault.get().equals(secretJsonObjectFromEntity.getVersion())) {
                    LOGGER.info("Version in vault differs ({}) for secret path {}, updating secret field!",
                            latestVersionFromVault.get(), secretJsonObjectFromEntity.getPath());
                    VaultSecret newVaultSecret = new VaultSecret(secretJsonObjectFromEntity.getEnginePath(), secretJsonObjectFromEntity.getEngineClass(),
                            secretJsonObjectFromEntity.getPath(), latestVersionFromVault.get());
                    String newSecretJsonEntity = new Gson().toJson(newVaultSecret);
                    VaultRotationReflectionUtil.setNewSecret(entity, secretMarker, new SecretProxy(newSecretJsonEntity));
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to compare secret versions, reason: ", e);
            throw new CloudbreakServiceException("Failed to compare secret versions to update vault secret in database!");
        }
        return false;
    }
}
