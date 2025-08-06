package com.sequenceiq.freeipa.service.rotation.salt.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.SaltSecurityConfigService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class SaltMasterKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMasterKeyPairRotationContextProvider.class);

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
        return FreeIpaSecretType.SALT_MASTER_KEY_PAIR;
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        generateAndStoreSaltMasterKeyPairIfMissing(saltSecurityConfig);
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltSecurityConfig))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, environmentCrn.getAccountId(), stack.getId()))
                .build();
    }

    private void generateAndStoreSaltMasterKeyPairIfMissing(SaltSecurityConfig saltSecurityConfig) {
        if (StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKeyVault())) {
            LOGGER.info("Salt master private key is missing from the database and Vault, generate and store a new private key.");
            saltSecurityConfig.setSaltMasterPrivateKeyVault(PkiUtil.generatePemPrivateKeyInBase64());
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private VaultRotationContext getVaultRotationContext(String environmentCrn, SaltSecurityConfig saltSecurityConfig) {
        Map<String, String> newSecretMap = Map.of(saltSecurityConfig.getSaltMasterPrivateKeyVaultSecret(), PkiUtil.generatePemPrivateKeyInBase64());
        return VaultRotationContext.builder()
                .withResourceCrn(environmentCrn)
                .withNewSecretMap(newSecretMap)
                .withEntitySaverList(List.of(() -> saltSecurityConfigService.save(saltSecurityConfig)))
                .withEntitySecretFieldUpdaterMap(Map.of(saltSecurityConfig.getSaltMasterPrivateKeyVaultSecret(),
                        vaultSecretJson -> saltSecurityConfig.setSaltMasterPrivateKeyVault(new SecretProxy(vaultSecretJson))))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String accountId, Long stackId) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn, accountId))
                .withRotationJob(() -> updateSaltMasterKeyPairOnCluster(stackId))
                .withRollbackJob(() -> updateSaltMasterKeyPairOnCluster(stackId))
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
        if (saltSecurityConfig.getLegacySaltMasterPublicKey() != null) {
            saltSecurityConfig.setSaltMasterPublicKey(null);
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private void updateSaltMasterKeyPairOnCluster(Long stackId) {
        try {
            LOGGER.info("Bootstrap machines to upload new salt master key pair based on the rotation phase");
            bootstrapService.bootstrap(stackId, true);
            LOGGER.info("Bootstrapping and restarting salt finished, salt master key pair uploaded.");
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Bootstrapping and restarting salt failed", e);
            throw new SecretRotationException(e.getMessage(), e);
        }
    }
}
