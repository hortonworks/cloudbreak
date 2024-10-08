package com.sequenceiq.cloudbreak.rotation.context.provider;

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
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class SaltSignKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltSignKeyPairRotationContextProvider.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.SALT_SIGN_KEY_PAIR;
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        String saltSignPrivateKeySecretPath = saltSecurityConfig.getSaltSignPrivateKeySecret().getSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltSignPrivateKeySecretPath))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, saltSignPrivateKeySecretPath))
                .build();
    }

    private VaultRotationContext getVaultRotationContext(String resourceCrn, String saltSignPrivateKeySecretPath) {
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(Map.of(saltSignPrivateKeySecretPath,
                        BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes())))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String saltSignPrivateKeySecretPath) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn))
                .withRotationJob(() -> updateSaltSignKeyPair(resourceCrn, RotationSecret::getSecret))
                .withRollbackJob(() -> updateSaltSignKeyPair(resourceCrn, RotationSecret::getBackupSecret))
                .build();
    }

    private void validateAllInstancesAreReachable(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        List<String> unreachableInstances = stack.getAllAvailableInstances().stream()
                .filter(instance -> !instance.isReachable())
                .map(InstanceMetadataView::getInstanceId)
                .toList();
        if (!unreachableInstances.isEmpty()) {
            throw new SecretRotationException(String.format("Unreachable instances found: %s, salt sign key rotation is not possible!", unreachableInstances));
        }
    }

    private void updateSaltSignKeyPair(String resourceCrn, Function<RotationSecret, String> mapper) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        updateSaltSignPublicKeyBasedOnPrivateKeyInVault(stack, mapper);
        runSaltBootstrap(stack.getId());
    }

    private void updateSaltSignPublicKeyBasedOnPrivateKeyInVault(StackDto stack, Function<RotationSecret, String> mapper) {
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        RotationSecret saltSignPrivateKey = uncachedSecretServiceForRotation.getRotation(saltSecurityConfig.getSaltSignPrivateKeySecret().getSecret());
        saltSecurityConfig.setSaltSignPublicKey(PkiUtil.calculatePemPublicKeyInBase64(mapper.apply(saltSignPrivateKey)));
        saltSecurityConfigService.save(saltSecurityConfig);
    }

    private void runSaltBootstrap(Long stackId) {
        try {
            clusterBootstrapper.bootstrapMachines(stackId, true);
            LOGGER.info("Bootstrapping and restarting salt finished.");
        } catch (CloudbreakException e) {
            LOGGER.warn("Bootstrapping and restarting salt failed", e);
            throw new SecretRotationException(e);
        }
    }
}
