package com.sequenceiq.cloudbreak.rotation.vault;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Component
public class VaultRotationExecutor extends AbstractRotationExecutor<VaultRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationExecutor.class);

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            String newSecret = entry.getValue();
            if (!secretService.getRotation(vaultPath).isRotation()) {
                LOGGER.info("Adding new secret to vault path {}", vaultPath);
                secretService.putRotation(vaultPath, newSecret);
            }
        }
    }

    @Override
    public void rollback(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            RotationSecret rotationSecret = secretService.getRotation(vaultPath);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing new secret from vault path {}", vaultPath);
                secretService.update(vaultPath, rotationSecret.getBackupSecret());
            }
        }
    }

    @Override
    public void finalize(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            RotationSecret rotationSecret = secretService.getRotation(vaultPath);
            if (rotationSecret.isRotation()) {
                LOGGER.info("Removing old secret from vault path {}", vaultPath);
                secretService.update(vaultPath, rotationSecret.getSecret());
            }
        }
    }

    @Override
    public void preValidate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            if (!secretService.isSecret(vaultPath)) {
                throw new SecretRotationException(String.format("%s is not a vault path, thus rotation is not possible!", vaultPath), getType());
            }
        }
    }

    @Override
    public void postValidate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            if (!secretService.getRotation(vaultPath).isRotation()) {
                String message = String.format("%s vault path is not in rotation state, thus something went wrong during rotation!", vaultPath);
                throw new SecretRotationException(message, getType());
            }
        }
    }

    @Override
    public SecretRotationStep getType() {
        return CommonSecretRotationStep.VAULT;
    }

    @Override
    public Class<VaultRotationContext> getContextClass() {
        return VaultRotationContext.class;
    }
}
