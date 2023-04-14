package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class VaultRotationExecutor implements RotationExecutor {

    @Inject
    private SecretService secretService;

    @Override
    public void rotate(RotationContext rotationContext) {
        rotationContext.getUserPasswordSecrets().forEach((userSecret, passwordSecret) -> {
            try {
                secretService.update(passwordSecret, PasswordUtil.generatePassword());
            } catch (Exception e) {
                throw new SecretRotationException(e, getType());
            }
        });
    }

    @Override
    public void rollback(RotationContext rotationContext) {
        // TODO remove new secret and use previous one
    }

    @Override
    public void finalize(RotationContext rotationContext) {
        // TODO not sure, we can keep old one in Vault or remove if not needed anymore
    }

    @Override
    public SecretLocationType getType() {
        return SecretLocationType.VAULT;
    }
}
