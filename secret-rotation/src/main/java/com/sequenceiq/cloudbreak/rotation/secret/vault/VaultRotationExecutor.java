package com.sequenceiq.cloudbreak.rotation.secret.vault;

import static com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationReflectionUtil.getVaultSecretJson;
import static com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationReflectionUtil.saveEntity;
import static com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationReflectionUtil.setNewSecret;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.function.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;

@Component
public class VaultRotationExecutor extends AbstractRotationExecutor<VaultRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationExecutor.class);

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Override
    protected void rotate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((entity, markerMap) -> {
            performVaultRotationPhase(markerMap, entity, (marker, newValue, vaultSecretJson) -> {
                if (!uncachedSecretServiceForRotation.getRotation(vaultSecretJson).isRotation()) {
                    LOGGER.info("Adding new secret to vault path {}", vaultSecretJson);
                    String newVaultSecretJson = uncachedSecretServiceForRotation.putRotation(vaultSecretJson, newValue);
                    setNewSecret(entity, marker, new SecretProxy(newVaultSecretJson));
                }
            });
            saveEntity(entity);
        });
    }

    @Override
    protected void rollback(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((entity, markerMap) -> {
            performVaultRotationPhase(markerMap, entity, (marker, newValue, vaultSecretJson) -> {
                RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultSecretJson);
                if (rotationSecret.isRotation()) {
                    LOGGER.info("Removing new secret from vault path {}", vaultSecretJson);
                    String rolledBackVaultSecretJson = uncachedSecretServiceForRotation.update(vaultSecretJson, rotationSecret.getBackupSecret());
                    setNewSecret(entity, marker, new SecretProxy(rolledBackVaultSecretJson));
                }
            });
            saveEntity(entity);
        });
    }

    @Override
    protected void finalizeRotation(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((entity, markerMap) -> {
            performVaultRotationPhase(markerMap, entity, (marker, newValue, vaultSecretJson) -> {
                RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultSecretJson);
                if (rotationSecret.isRotation()) {
                    LOGGER.info("Removing old secret from vault path {}", vaultSecretJson);
                    String finalizedVaultSecretJson = uncachedSecretServiceForRotation.update(vaultSecretJson, rotationSecret.getSecret());
                    setNewSecret(entity, marker, new SecretProxy(finalizedVaultSecretJson));
                }
            });
            saveEntity(entity);
        });
    }

    @Override
    protected void preValidate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((entity, markerMap) -> {
            performVaultRotationPhase(markerMap, entity, (marker, newValue, vaultSecretJson) -> {
                if (!uncachedSecretServiceForRotation.isSecret(vaultSecretJson)) {
                    throw new SecretRotationException(String.format("%s is not a vault path, thus rotation is not possible!", vaultSecretJson));
                }
            });
        });
    }

    @Override
    protected void postValidate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((entity, markerMap) -> {
            performVaultRotationPhase(markerMap, entity, (marker, newValue, vaultSecretJson) -> {
                if (!uncachedSecretServiceForRotation.getRotation(vaultSecretJson).isRotation()) {
                    String message = String.format("%s vault path is not in rotation state, thus something went wrong during rotation!", vaultSecretJson);
                    throw new SecretRotationException(message);
                }
            });
        });
    }

    private void performVaultRotationPhase(Map<SecretMarker, String> markerMap, Object entity,
            TriConsumer<SecretMarker, String, String> consumer) {
        markerMap.forEach((marker, newValue) -> {
            try {
                String vaultSecretJson = getVaultSecretJson(entity, marker);
                consumer.accept(marker, newValue, vaultSecretJson);
            } catch (Exception e) {
                throw new SecretRotationException(e);
            }
        });
    }

    @Override
    public SecretRotationStep getType() {
        return CommonSecretRotationStep.VAULT;
    }

    @Override
    protected Class<VaultRotationContext> getContextClass() {
        return VaultRotationContext.class;
    }
}
