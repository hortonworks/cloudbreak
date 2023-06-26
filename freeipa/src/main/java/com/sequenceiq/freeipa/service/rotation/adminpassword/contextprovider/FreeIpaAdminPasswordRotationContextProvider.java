package com.sequenceiq.freeipa.service.rotation.adminpassword.contextprovider;

import static com.sequenceiq.freeipa.api.rotation.FreeIpaSecretType.FREEIPA_ADMIN_PASSWORD;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.rotation.pillar.context.FreeipaPillarRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaAdminPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stackByResourceCrn = stackService.getByEnvironmentCrnAndAccountId(resourceCrn, environmentCrn.getAccountId());
        FreeIpa freeIpa = freeIpaService.findByStack(stackByResourceCrn);
        String newPassword = FreeIpaPasswordUtil.generatePassword();
        Map<String, String> vaultPathMap = Maps.newHashMap();
        Secret adminPasswordSecret = freeIpa.getAdminPasswordSecret();
        vaultPathMap.put(adminPasswordSecret.getSecret(), newPassword);
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(vaultPathMap)
                .build();

        FreeIpaAdminPasswordRotationContext freeipaAdminPasswordRotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret.getSecret())
                .build();

        FreeipaPillarRotationContext freeipaPillarRotationContext = FreeipaPillarRotationContext.builder().withResourceCrn(resourceCrn).build();

        result.put(CommonSecretRotationStep.VAULT, vaultRotationContext);
        result.put(FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD, freeipaAdminPasswordRotationContext);
        result.put(FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD, freeipaAdminPasswordRotationContext);
        result.put(FreeIpaSecretRotationStep.FREEIPA_PILLAR_UPDATE, freeipaPillarRotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return FREEIPA_ADMIN_PASSWORD;
    }
}
