package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.Map;

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
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            String newSecret = entry.getValue();
            if (!uncachedSecretServiceForRotation.getRotation(vaultPath).isRotation()) {
                LOGGER.info("Adding new secret to vault path {}", vaultPath);
                uncachedSecretServiceForRotation.putRotation(vaultPath, newSecret);
            }
        }
    }

    @Override
    protected void rollback(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultPath);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing new secret from vault path {}", vaultPath);
                uncachedSecretServiceForRotation.update(vaultPath, rotationSecret.getBackupSecret());
            }
        }
    }

    @Override
    protected void finalizeRotation(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultPath);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing old secret from vault path {}", vaultPath);
                uncachedSecretServiceForRotation.update(vaultPath, rotationSecret.getSecret());
            }
        }
    }

    @Override
    protected void preValidate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            if (!uncachedSecretServiceForRotation.isSecret(vaultPath)) {
                throw new SecretRotationException(String.format("%s is not a vault path, thus rotation is not possible!", vaultPath));
            }
        }
    }

    @Override
    protected void postValidate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            if (!uncachedSecretServiceForRotation.getRotation(vaultPath).isRotation()) {
                String message = String.format("%s vault path is not in rotation state, thus something went wrong during rotation!", vaultPath);
                throw new SecretRotationException(message);
            }
        }
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
