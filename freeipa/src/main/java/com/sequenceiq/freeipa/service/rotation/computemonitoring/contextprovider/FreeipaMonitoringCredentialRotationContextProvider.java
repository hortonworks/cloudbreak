package com.sequenceiq.freeipa.service.rotation.computemonitoring.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.COMPUTE_MONITORING_CREDENTIALS;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.computemonitoring.service.FreeipaMonitoringCredentialsRotationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaMonitoringCredentialRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackService stackService;

    @Inject
    private FreeipaMonitoringCredentialsRotationService rotationService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, ThreadBasedUserCrnProvider.getAccountId());
        RotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> rotationService.validateEnablement(stack))
                .withRotationJob(() -> rotationService.updateMonitoringCredentials(stack))
                .withRollbackJob(() -> rotationService.updateMonitoringCredentials(stack))
                .build();
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of(stack.getCdpNodeStatusMonitorPasswordSecret().getSecret(), PasswordUtil.generatePassword()))
                .withResourceCrn(resourceCrn)
                .build();
        return Map.of(CUSTOM_JOB, customJobRotationContext,
                VAULT, vaultRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return COMPUTE_MONITORING_CREDENTIALS;
    }
}
