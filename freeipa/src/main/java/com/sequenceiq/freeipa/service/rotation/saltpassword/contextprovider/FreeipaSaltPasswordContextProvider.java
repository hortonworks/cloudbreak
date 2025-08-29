package com.sequenceiq.freeipa.service.rotation.saltpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.SaltSecurityConfigService;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaSaltPasswordContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(resourceCrn, environmentCrn.getAccountId());
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        Map<String, String> newSecretMap = Map.of(saltSecurityConfig.getSaltPasswordVaultSecret(), PasswordUtil.generatePassword());
        return Map.of(VAULT, VaultRotationContext.builder()
                        .withResourceCrn(resourceCrn)
                        .withNewSecretMap(newSecretMap)
                        .withEntitySaverList(List.of(() -> saltSecurityConfigService.save(saltSecurityConfig)))
                        .withEntitySecretFieldUpdaterMap(Map.of(saltSecurityConfig.getSaltPasswordVaultSecret(),
                                vaultSecretJson -> saltSecurityConfig.setSaltPasswordVault(new SecretProxy(vaultSecretJson))))
                        .build(),
                CUSTOM_JOB, getCustomJobRotationContext(stack));
    }

    private CustomJobRotationContext getCustomJobRotationContext(Stack stack) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(stack.getEnvironmentCrn())
                .withRotationJob(() -> rotateSaltPasswordService.rotateSaltPassword(stack))
                .withPostValidateJob(() -> {
                    rotateSaltPasswordService.validatePasswordAfterRotation(stack);
                    secretRotationSaltService.validateSalt(stack);
                })
                .build();
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.SALT_PASSWORD;
    }
}
