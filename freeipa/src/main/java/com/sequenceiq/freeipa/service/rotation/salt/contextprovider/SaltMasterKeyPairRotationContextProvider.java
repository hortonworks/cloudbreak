package com.sequenceiq.freeipa.service.rotation.salt.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
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
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.SaltSecurityConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class SaltMasterKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMasterKeyPairRotationContextProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private BootstrapService bootstrapService;

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
        String saltMasterPrivateKeySecretPath = saltSecurityConfig.getSaltMasterPrivateKeyVaultSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltMasterPrivateKeySecretPath))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, environmentCrn.getAccountId(), saltMasterPrivateKeySecretPath))
                .build();
    }

    private void generateAndStoreSaltMasterKeyPairIfMissing(SaltSecurityConfig saltSecurityConfig) {
        if (StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKeyVault())) {
            LOGGER.info("Salt master key pair is missing from the database, generate and store new key pair");
            KeyPair keyPair = PkiUtil.generateKeypair();
            saltSecurityConfig.setSaltMasterPublicKey(BaseEncoding.base64().encode(PkiUtil.convertPemPublicKey(keyPair.getPublic()).getBytes()));
            saltSecurityConfig.setSaltMasterPrivateKeyVault(BaseEncoding.base64().encode(PkiUtil.convert(keyPair.getPrivate()).getBytes()));
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private VaultRotationContext getVaultRotationContext(String environmentCrn, String saltMasterPrivateKeySecretPath) {
        return VaultRotationContext.builder()
                .withResourceCrn(environmentCrn)
                .withVaultPathSecretMap(Map.of(saltMasterPrivateKeySecretPath,
                        BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes())))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String accountId, String saltMasterPrivateKeySecretPath) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn, accountId))
                .withRotationJob(() -> updateSaltMasterKeyPair(resourceCrn, accountId, RotationSecret::getSecret))
                .withRollbackJob(() -> updateSaltMasterKeyPair(resourceCrn, accountId, RotationSecret::getBackupSecret))
                .build();
    }

    private void validateAllInstancesAreReachable(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        List<String> unavailableInstances = stack.getAllInstanceMetaDataList().stream()
                .filter(instance -> !instance.isAvailable())
                .map(InstanceMetaData::getInstanceId)
                .toList();
        if (!unavailableInstances.isEmpty()) {
            throw new SecretRotationException(String.format("Unavailable instances found: %s, salt master key rotation is not possible!", unavailableInstances));
        }
    }

    private void updateSaltMasterKeyPair(String resourceCrn, String accountId, Function<RotationSecret, String> mapper) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, accountId);
        updateSaltMasterPublicKeyBasedOnPrivateKeyInVault(stack, mapper);
        runSaltBootstrap(stack.getId());
    }

    private void updateSaltMasterPublicKeyBasedOnPrivateKeyInVault(Stack stack, Function<RotationSecret, String> mapper) {
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        RotationSecret saltMasterPrivateKey = uncachedSecretServiceForRotation.getRotation(saltSecurityConfig.getSaltMasterPrivateKeyVaultSecret());
        saltSecurityConfig.setSaltMasterPublicKey(PkiUtil.calculatePemPublicKeyInBase64(mapper.apply(saltMasterPrivateKey)));
        saltSecurityConfigService.save(saltSecurityConfig);
    }

    private void runSaltBootstrap(Long stackId) {
        try {
            bootstrapService.bootstrap(stackId, true);
            LOGGER.info("Bootstrapping and restarting salt finished.");
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Bootstrapping and restarting salt failed", e);
            throw new SecretRotationException(e);
        }
    }
}
