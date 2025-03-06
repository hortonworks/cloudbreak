package com.sequenceiq.freeipa.service.rotation.executor;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaUserPasswordRotationExecutor extends AbstractRotationExecutor<FreeIpaUserPasswordRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserPasswordRotationExecutor.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Override
    public void rotate(FreeIpaUserPasswordRotationContext rotationContext) {
        RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getPasswordSecret());
        if (passwordRotationSecret.isRotation()) {
            String newPassword = passwordRotationSecret.getSecret();
            changePassword(rotationContext, newPassword);
        } else {
            throw new SecretRotationException("Freeipa user password is not in rotation state in Vault, thus rotation of user password is not possible.");
        }
    }

    @Override
    public void rollback(FreeIpaUserPasswordRotationContext rotationContext) {
        RotationSecret passwordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getPasswordSecret());
        if (passwordRotationSecret.isRotation()) {
            String oldPassword = passwordRotationSecret.getBackupSecret();
            changePassword(rotationContext, oldPassword);
        } else {
            throw new SecretRotationException("Freeipa user password is not in rotation state in Vault, thus rolling back of user password is not possible.");
        }
    }

    private void changePassword(FreeIpaUserPasswordRotationContext rotationContext, String password) {
        try {
            Crn environmentCrn = Crn.safeFromString(rotationContext.getResourceCrn());
            Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(rotationContext.getResourceCrn(), environmentCrn.getAccountId());
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            User user = freeIpaClient.userShow(rotationContext.getUsername());
            freeIpaClient.userSetPasswordWithExpiration(user.getUid(), password, Optional.empty());
        } catch (FreeIpaClientException fce) {
            throw new SecretRotationException("Freeipa client creation failed, ", fce);
        } catch (Exception e) {
            throw new SecretRotationException("Password renewal failed, ", e);
        }
    }

    @Override
    public void finalizeRotation(FreeIpaUserPasswordRotationContext rotationContext) {

    }

    @Override
    public void preValidate(FreeIpaUserPasswordRotationContext rotationContext) {

    }

    @Override
    public void postValidate(FreeIpaUserPasswordRotationContext rotationContext) {

    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;
    }

    @Override
    public Class<FreeIpaUserPasswordRotationContext> getContextClass() {
        return FreeIpaUserPasswordRotationContext.class;
    }
}
