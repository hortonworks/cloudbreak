package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.COMPUTE_MONITORING_CREDENTIALS;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.MonitoringCredentialsRotationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class MonitoringCredentialsRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private MonitoringCredentialsRotationService rotationService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        RotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> rotationService.validateEnablement(stackDto))
                .withRotationJob(() -> rotationService.updateMonitoringCredentials(stackDto))
                .withRollbackJob(() -> rotationService.updateMonitoringCredentials(stackDto))
                .build();
        ClusterView cluster = stackDto.getCluster();
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of(
                        cluster.getCloudbreakClusterManagerMonitoringUserSecret().getSecret(), rotationService.getCmMonitoringUser(),
                        cluster.getCloudbreakClusterManagerMonitoringPasswordSecret().getSecret(), PasswordUtil.generatePassword(),
                        cluster.getCdpNodeStatusMonitorPasswordSecret().getSecret(), PasswordUtil.generatePassword()))
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
