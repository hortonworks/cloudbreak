package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.AdminUserService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaAdminPasswordRotationExecutor extends AbstractRotationExecutor<FreeIpaAdminPasswordRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAdminPasswordRotationExecutor.class);

    @Inject
    private AdminUserService adminUserService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private FreeIpaAdminPasswordRotationUtil freeIpaAdminPasswordRotationUtil;

    @Override
    public void rotate(FreeIpaAdminPasswordRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        RotationSecret adminPasswordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getAdminPasswordSecret());
        if (adminPasswordRotationSecret.isRotation()) {
            try {
                String newPassword = adminPasswordRotationSecret.getSecret();
                ThreadBasedVaultReadFieldProvider.doWithBackup(Set.of(rotationContext.getAdminPasswordSecret()), () -> {
                    try {
                        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
                        adminUserService.updateAdminUserPassword(newPassword, freeIpaClient);
                    } catch (FreeIpaClientException e) {
                        LOGGER.info("Freeipa client can not be created for admin password update", e);
                        throw new CloudbreakRuntimeException("Freeipa client can not be created for admin password update", e);
                    }
                });
                ThreadBasedVaultReadFieldProvider.doWithNewSecret(Set.of(rotationContext.getAdminPasswordSecret()),
                        () -> adminUserService.waitAdminUserPasswordReplication(stack));
            } catch (Exception e) {
                LOGGER.info("Rotation of freeipa admin password failed", e);
                throw new SecretRotationException("Freeipa admin password rotation failed", e);
            }
        } else {
            throw new SecretRotationException("Freeipa admin password is not in rotation state in Vault, thus rotation of admin password " +
                    "is not possible.");
        }
    }

    @Override
    public void rollback(FreeIpaAdminPasswordRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        RotationSecret adminPasswordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getAdminPasswordSecret());
        String backupPassword = adminPasswordRotationSecret.getBackupSecret();
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        try {
            freeIpaClientFactory.getFreeIpaClientForStack(stack);
            LOGGER.info("We were able to create client with the backup secret, so we did not modify the admin password, therefore " +
                    "we should not rollback anything");
        } catch (FreeIpaClientException exceptionForBackupSecret) {
            LOGGER.info("Can not create freeipa client with backup secret, let's create it with the new secret", exceptionForBackupSecret);
            ThreadBasedVaultReadFieldProvider.doWithNewSecret(Set.of(rotationContext.getAdminPasswordSecret()), () -> {
                try {
                    FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
                    LOGGER.info("We were able to create client with the new secret, we should do the rollback steps");
                    adminUserService.updateAdminUserPassword(backupPassword, freeIpaClient);
                } catch (FreeIpaClientException exceptionWithNewSecret) {
                    LOGGER.info("Can not create freeipa client with the new secret", exceptionWithNewSecret);
                    throw new CloudbreakRuntimeException("The attempt to revert the rotation has been unsuccessful. We are unable to create a client using " +
                            "either the new password or the old password.", exceptionWithNewSecret);
                }
                ThreadBasedVaultReadFieldProvider.doWithBackup(Set.of(rotationContext.getAdminPasswordSecret()),
                        () -> adminUserService.waitAdminUserPasswordReplication(stack));
            });
        }
    }

    @Override
    public void finalizeRotation(FreeIpaAdminPasswordRotationContext rotationContext) {

    }

    @Override
    public void preValidate(FreeIpaAdminPasswordRotationContext rotationContext) {
        freeIpaAdminPasswordRotationUtil.checkRedhat8(rotationContext);
    }

    @Override
    public void postValidate(FreeIpaAdminPasswordRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        ThreadBasedVaultReadFieldProvider.doWithNewSecret(Set.of(rotationContext.getAdminPasswordSecret()), () -> {
            try {
                freeIpaClientFactory.getFreeIpaClientForStack(stack);
                LOGGER.info("We were able to create client with the new secret, we can finalize the rotation");
            } catch (FreeIpaClientException exceptionWithNewSecret) {
                LOGGER.info("Can not create freeipa client with the new secret", exceptionWithNewSecret);
                throw new CloudbreakRuntimeException("The attempt to validate the new password has been unsuccessful. We are unable to create a client " +
                        "using the new password", exceptionWithNewSecret);
            }
        });
    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD;
    }

    @Override
    public Class<FreeIpaAdminPasswordRotationContext> getContextClass() {
        return FreeIpaAdminPasswordRotationContext.class;
    }
}
