package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.secret.service.rotation.context.VaultRotationContext;

@Component
public class VaultRotationExecutor implements RotationExecutor<VaultRotationContext> {

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(VaultRotationContext rotationContext) {
        rotationContext.getNewSecretMap().forEach((vaultSecret, newSecretValue) -> {
            try {
                secretService.update(vaultSecret.getSecret(), newSecretValue);
            } catch (Exception e) {
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void rollback(VaultRotationContext rotationContext) {
        // TODO remove new secret and use previous one
    }

    @Override
    public void finalize(VaultRotationContext rotationContext) {
        // TODO not sure, we can keep old one in Vault or remove if not needed anymore
    }

    @Override
    public SecretLocationType getType() {
        return SecretLocationType.VAULT;
    }
}
