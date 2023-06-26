package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.secret.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.vault.ThreadBasedVaultReadFieldProvider;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.user.DirectoryManagerUserService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaDirectoryManagerPasswordRotationExecutor extends AbstractRotationExecutor<FreeIpaAdminPasswordRotationContext> {

    @Inject
    private DirectoryManagerUserService directoryManagerUserService;

    @Inject
    private StackService stackService;

    @Inject
    private SecretService secretService;

    @Inject
    private FreeIpaAdminPasswordRotationUtil freeIpaAdminPasswordRotationUtil;

    @Override
    public void rotate(FreeIpaAdminPasswordRotationContext rotationContext) throws Exception {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        RotationSecret adminPasswordRotationSecret = secretService.getRotation(rotationContext.getAdminPasswordSecret());
        if (adminPasswordRotationSecret.isRotation()) {
            String newPassword = adminPasswordRotationSecret.getSecret();
            ThreadBasedVaultReadFieldProvider.doWithBackup(Set.of(rotationContext.getAdminPasswordSecret()),
                    () -> directoryManagerUserService.updateDirectoryManagerPassword(stack, newPassword));
        } else {
            throw new SecretRotationException("Freeipa admin password is not in rotation state in Vault, thus rotation of directory manager password " +
                    "is not possible.", getType());
        }
    }

    @Override
    public void rollback(FreeIpaAdminPasswordRotationContext rotationContext) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        RotationSecret adminPasswordRotationSecret = secretService.getRotation(rotationContext.getAdminPasswordSecret());
        String backupPassword = adminPasswordRotationSecret.getBackupSecret();
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        try {
            directoryManagerUserService.updateDirectoryManagerPassword(stack, backupPassword);
        } catch (Exception e) {
            ThreadBasedVaultReadFieldProvider.doWithNewSecret(Set.of(rotationContext.getAdminPasswordSecret()), () -> {
                directoryManagerUserService.updateDirectoryManagerPassword(stack, backupPassword);
            });
        }
    }

    @Override
    public void finalize(FreeIpaAdminPasswordRotationContext rotationContext) {

    }

    @Override
    public void preValidate(FreeIpaAdminPasswordRotationContext rotationContext) {
        freeIpaAdminPasswordRotationUtil.checkRedhat8(rotationContext, getType());
    }

    @Override
    public void postValidate(FreeIpaAdminPasswordRotationContext rotationContext) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        directoryManagerUserService.checkDirectoryManagerPassword(stack);
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
