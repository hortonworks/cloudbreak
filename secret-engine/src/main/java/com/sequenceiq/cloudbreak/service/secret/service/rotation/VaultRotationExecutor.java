package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Component
public class VaultRotationExecutor implements RotationExecutor<VaultRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationExecutor.class);

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(VaultRotationContext rotationContext) throws Exception {
        for (Map.Entry<String, String> entry : rotationContext.getVaultPathSecretMap().entrySet()) {
            String vaultPath = entry.getKey();
            String newSecret = entry.getValue();
            if (!secretService.getRotation(vaultPath).isRotation()) {
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
                secretService.update(vaultPath, rotationSecret.getSecret());
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
