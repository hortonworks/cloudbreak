package com.sequenceiq.cloudbreak.rotation.secret.vault;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;

@Component
public class VaultRotationExecutor extends AbstractRotationExecutor<VaultRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationExecutor.class);

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Override
    protected void rotate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((vaultSecretJson, newSecret) -> {
            if (!uncachedSecretServiceForRotation.getRotation(vaultSecretJson).isRotation()) {
                LOGGER.info("Adding new secret to vault path {}", vaultSecretJson);
                String newVaultSecretJson = uncachedSecretServiceForRotation.putRotation(vaultSecretJson, newSecret);
                rotationContext.getEntitySecretFieldUpdaterMap()
                        .getOrDefault(vaultSecretJson, vaultSecretJsonInput ->
                                LOGGER.error("Cannot update secret json {} for entity, since no method provided for it.", vaultSecretJsonInput))
                        .accept(newVaultSecretJson);
            }
        });
        rotationContext.getEntitySaverList().forEach(Runnable::run);
    }

    @Override
    protected void rollback(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((vaultSecretJson, newSecret) -> {
            RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultSecretJson);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing new secret from vault path {}", vaultSecretJson);
                String rolledBackVaultSecretJson = uncachedSecretServiceForRotation.update(vaultSecretJson, rotationSecret.getBackupSecret());
                rotationContext.getEntitySecretFieldUpdaterMap()
                        .getOrDefault(vaultSecretJson, vaultSecretJsonInput ->
                                LOGGER.error("Cannot update secret json {} for entity, since no method provided for it.", vaultSecretJsonInput))
                        .accept(rolledBackVaultSecretJson);
            }
        });
        rotationContext.getEntitySaverList().forEach(Runnable::run);
    }

    @Override
    protected void finalizeRotation(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((vaultSecretJson, newSecret) -> {
            RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultSecretJson);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing old secret from vault path {}", vaultSecretJson);
                String finalizedVaultSecretJson = uncachedSecretServiceForRotation.update(vaultSecretJson, rotationSecret.getSecret());
                rotationContext.getEntitySecretFieldUpdaterMap()
                        .getOrDefault(vaultSecretJson, vaultSecretJsonInput ->
                                LOGGER.error("Cannot update secret json {} for entity, since no method provided for it.", vaultSecretJsonInput))
                        .accept(finalizedVaultSecretJson);
            }
        });
        rotationContext.getEntitySaverList().forEach(Runnable::run);
    }

    @Override
    protected void preValidate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((vaultSecretJson, newSecret) -> {
            if (!uncachedSecretServiceForRotation.isSecret(vaultSecretJson)) {
                throw new SecretRotationException(String.format("%s is not a vault path, thus rotation is not possible!", vaultSecretJson));
            }
        });
    }

    @Override
    protected void postValidate(VaultRotationContext rotationContext) throws Exception {
        rotationContext.getNewSecretMap().forEach((vaultSecretJson, newSecret) -> {
            if (!uncachedSecretServiceForRotation.getRotation(vaultSecretJson).isRotation()) {
                String message = String.format("%s vault path is not in rotation state, thus something went wrong during rotation!", vaultSecretJson);
                throw new SecretRotationException(message);
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
