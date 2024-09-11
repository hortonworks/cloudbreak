package com.sequenceiq.freeipa.service.rotation.salt.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;

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
public class SaltSignKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltSignKeyPairRotationContextProvider.class);

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
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, environmentCrn.getAccountId(), saltSignPrivateKeySecretPath))
                .build();
    }

    private VaultRotationContext getVaultRotationContext(String environmentCrn, String saltSignPrivateKeySecretPath) {
        return VaultRotationContext.builder()
                .withResourceCrn(environmentCrn)
                .withVaultPathSecretMap(Map.of(saltSignPrivateKeySecretPath,
                        BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes())))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String accountId, String saltSignPrivateKeySecretPath) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn, accountId))
                .withRotationJob(() -> updateSaltSignKeyPair(resourceCrn, accountId, RotationSecret::getSecret))
                .withRollbackJob(() -> updateSaltSignKeyPair(resourceCrn, accountId, RotationSecret::getBackupSecret))
                .build();
    }

    private void validateAllInstancesAreReachable(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        List<String> unavailableInstances = stack.getAllInstanceMetaDataList().stream()
                .filter(instance -> !instance.isAvailable())
                .map(InstanceMetaData::getInstanceId)
                .toList();
        if (!unavailableInstances.isEmpty()) {
            throw new SecretRotationException(String.format("Unavailable instances found: %s, salt sign key rotation is not possible!", unavailableInstances));
        }
    }

    private void updateSaltSignKeyPair(String resourceCrn, String accountId, Function<RotationSecret, String> mapper) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, accountId);
        updateSaltSignPublicKeyBasedOnPrivateKeyInVault(stack, mapper);
        runSaltBootstrap(stack.getId());
    }

    private void updateSaltSignPublicKeyBasedOnPrivateKeyInVault(Stack stack, Function<RotationSecret, String> mapper) {
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        RotationSecret saltSignPrivateKey = uncachedSecretServiceForRotation.getRotation(saltSecurityConfig.getSaltSignPrivateKeyVaultSecret());
        saltSecurityConfig.setSaltSignPublicKey(PkiUtil.calculatePemPublicKeyInBase64(mapper.apply(saltSignPrivateKey)));
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
