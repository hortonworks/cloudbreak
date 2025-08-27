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
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.user.DirectoryManagerPasswordCheckFailedException;
import com.sequenceiq.freeipa.service.freeipa.user.DirectoryManagerPasswordUpdateFailedException;
import com.sequenceiq.freeipa.service.freeipa.user.DirectoryManagerUserService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaDirectoryManagerPasswordRotationExecutor extends AbstractRotationExecutor<FreeIpaAdminPasswordRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDirectoryManagerPasswordRotationExecutor.class);

    @Inject
    private DirectoryManagerUserService directoryManagerUserService;

    @Inject
    private StackService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private FreeIpaAdminPasswordRotationUtil freeIpaAdminPasswordRotationUtil;

    @Override
    public void rotate(FreeIpaAdminPasswordRotationContext rotationContext) throws Exception {
        LOGGER.info("Rotate directory manager password");
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        RotationSecret adminPasswordRotationSecret = uncachedSecretServiceForRotation.getRotation(rotationContext.getAdminPasswordSecret());
        if (adminPasswordRotationSecret.isRotation()) {
            String newPassword = adminPasswordRotationSecret.getSecret();
            ThreadBasedVaultReadFieldProvider.doWithBackup(Set.of(rotationContext.getAdminPasswordSecret()),
                    () -> {
                        try {
                            directoryManagerUserService.updateDirectoryManagerPassword(stack, newPassword);
                        } catch (DirectoryManagerPasswordUpdateFailedException e) {
                            throw new CloudbreakRuntimeException(e.getMessage(), e.getCause());
                        }
                    });
        } else {
            throw new SecretRotationException("Freeipa admin password is not in rotation state in Vault, thus rotation of directory manager password " +
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
            LOGGER.info("Check directory manager password with original password, if it works good, we are fine");
            directoryManagerUserService.checkDirectoryManagerPassword(stack);
        } catch (Exception e) {
            LOGGER.info("Try to update the directory manager password to the backup password with the new secret");
            ThreadBasedVaultReadFieldProvider.doWithNewSecret(Set.of(rotationContext.getAdminPasswordSecret()), () -> {
                try {
                    directoryManagerUserService.updateDirectoryManagerPassword(stack, backupPassword);
                } catch (DirectoryManagerPasswordUpdateFailedException ex) {
                    throw new CloudbreakRuntimeException(ex.getMessage(), ex.getCause());
                }
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
        LOGGER.info("Do validation for directory manager password rotation for: {}", stack.getResourceCrn());
        try {
            directoryManagerUserService.checkDirectoryManagerPassword(stack);
        } catch (DirectoryManagerPasswordCheckFailedException e) {
            throw new CloudbreakRuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD;
    }

    @Override
    public Class<FreeIpaAdminPasswordRotationContext> getContextClass() {
        return FreeIpaAdminPasswordRotationContext.class;
    }
}
