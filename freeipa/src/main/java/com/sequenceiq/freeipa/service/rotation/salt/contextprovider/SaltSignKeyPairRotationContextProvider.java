package com.sequenceiq.freeipa.service.rotation.salt.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.SaltSecurityConfigService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class SaltSignKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltSignKeyPairRotationContextProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private BootstrapService bootstrapService;

    @Inject
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.SALT_SIGN_KEY_PAIR;
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        String saltSignPrivateKeySecretPath = saltSecurityConfig.getSaltSignPrivateKeyVaultSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltSignPrivateKeySecretPath))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, environmentCrn.getAccountId(), stack.getId()))
                .build();
    }

    private VaultRotationContext getVaultRotationContext(String environmentCrn, String saltSignPrivateKeySecretPath) {
        return VaultRotationContext.builder()
                .withResourceCrn(environmentCrn)
                .withVaultPathSecretMap(Map.of(saltSignPrivateKeySecretPath, PkiUtil.generatePemPrivateKeyInBase64()))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String accountId, Long stackId) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn, accountId))
                .withRotationJob(() -> updateSaltSignKeyPairOnCluster(stackId))
                .withRollbackJob(() -> updateSaltSignKeyPairOnCluster(stackId))
                .withPostValidateJob(() -> validateAllInstancesAreReachable(resourceCrn, accountId))
                .withFinalizeJob(() -> deletePublicKeyFromDatabaseIfExists(resourceCrn, accountId))
                .build();
    }

    private void validateAllInstancesAreReachable(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        try {
            freeIpaSaltPingService.saltPing(stack);
        } catch (SaltPingFailedException e) {
            throw new SecretRotationException(e.getMessage(), e);
        }
    }

    private void deletePublicKeyFromDatabaseIfExists(String resourceCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, accountId);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        if (saltSecurityConfig.getLegacySaltSignPublicKey() != null) {
            saltSecurityConfig.setSaltSignPublicKey(null);
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private void updateSaltSignKeyPairOnCluster(Long stackId) {
        try {
            LOGGER.info("Bootstrap machines to upload new salt sign key pair based on the rotation phase");
            bootstrapService.bootstrap(stackId, true);
            LOGGER.info("Bootstrapping and restarting salt finished, salt sign key pair uploaded.");
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Bootstrapping and restarting salt failed", e);
            throw new SecretRotationException(e.getMessage(), e);
        }
    }
}
