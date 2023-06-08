package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
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
    public void rotate(VaultRotationContext rotationContext) {
        rotationContext.getVaultPathSecretMap().forEach((vaultPath, newSecret) -> {
            try {
                if (!secretService.getRotation(vaultPath).isRotation()) {
                    secretService.putRotation(vaultPath, newSecret);
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret rotation.", vaultPath, e);
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void rollback(VaultRotationContext rotationContext) {
        rotationContext.getVaultPathSecretMap().forEach((vaultPath, newSecret) -> {
            try {
                RotationSecret rotationSecret = secretService.getRotation(vaultPath);
                if (rotationSecret.isRotation()) {
                    secretService.update(vaultPath, rotationSecret.getBackupSecret());
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret rollback.", vaultPath, e);
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void finalize(VaultRotationContext rotationContext) {
        rotationContext.getVaultPathSecretMap().forEach((vaultPath, newSecret) -> {
            try {
                RotationSecret rotationSecret = secretService.getRotation(vaultPath);
                if (rotationSecret.isRotation()) {
                    secretService.update(vaultPath, rotationSecret.getSecret());
                }
            } catch (Exception e) {
                LOGGER.error("Error during {} secret finalization.", vaultPath, e);
                throw new SecretRotationException(e, getType());
            }
        });
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
