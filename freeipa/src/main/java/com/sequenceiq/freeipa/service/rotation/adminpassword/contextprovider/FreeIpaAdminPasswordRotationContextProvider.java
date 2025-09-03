package com.sequenceiq.freeipa.service.rotation.adminpassword.contextprovider;

import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_ADMIN_PASSWORD;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaDefaultPillarGenerator;
import com.sequenceiq.freeipa.service.rotation.context.SaltPillarUpdateRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaAdminPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaDefaultPillarGenerator freeIpaDefaultPillarGenerator;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String environmentCrnAsString) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stackByResourceCrn = stackService.getByEnvironmentCrnAndAccountId(environmentCrnAsString, environmentCrn.getAccountId());
        FreeIpa freeIpa = freeIpaService.findByStack(stackByResourceCrn);
        String newPassword = FreeIpaPasswordUtil.generatePassword();

        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(environmentCrnAsString)
                .withNewSecretMap(Map.of(freeIpa, Map.of(SecretMarker.FREEIPA_ADMIN_PASSWORD, newPassword)))
                .build();

        RotationContext freeipaAdminPasswordRotationContext = new RotationContext(environmentCrnAsString);

        SaltPillarUpdateRotationContext freeipaPillarRotationContext = SaltPillarUpdateRotationContext.builder()
                .withEnvironmentCrn(environmentCrnAsString)
                .withServicePillarGenerator(freeIpaDefaultPillarGenerator)
                .build();

        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        result.put(CommonSecretRotationStep.VAULT, vaultRotationContext);
        result.put(FreeIpaSecretRotationStep.FREEIPA_ADMIN_USER_PASSWORD, freeipaAdminPasswordRotationContext);
        result.put(FreeIpaSecretRotationStep.FREEIPA_DIRECTORY_MANAGER_PASSWORD, freeipaAdminPasswordRotationContext);
        result.put(FreeIpaSecretRotationStep.SALT_PILLAR_UPDATE, freeipaPillarRotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return FREEIPA_ADMIN_PASSWORD;
    }
}
