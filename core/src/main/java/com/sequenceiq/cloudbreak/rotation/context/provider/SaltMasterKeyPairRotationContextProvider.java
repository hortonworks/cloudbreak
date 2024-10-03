package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.security.KeyPair;
import java.util.HashMap;
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
public class SaltMasterKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMasterKeyPairRotationContextProvider.class);

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

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
        return CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        generateAndStoreSaltMasterKeyPairIfMissing(saltSecurityConfig);
        String saltMasterPrivateKeySecretPath = saltSecurityConfig.getSaltMasterPrivateKeySecret().getSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltMasterPrivateKeySecretPath))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, saltMasterPrivateKeySecretPath))
                .build();
    }

    private void generateAndStoreSaltMasterKeyPairIfMissing(SaltSecurityConfig saltSecurityConfig) {
        if (StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKey())) {
            LOGGER.info("Salt master key pair is missing from the database, generate and store new key pair");
            KeyPair keyPair = PkiUtil.generateKeypair();
            saltSecurityConfig.setSaltMasterPublicKey(BaseEncoding.base64().encode(PkiUtil.convertOpenSshPublicKey(keyPair.getPublic()).getBytes()));
            saltSecurityConfig.setSaltMasterPrivateKey(BaseEncoding.base64().encode(PkiUtil.convert(keyPair.getPrivate()).getBytes()));
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private VaultRotationContext getVaultRotationContext(String resourceCrn, String saltMasterPrivateKeySecretPath) {
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(Map.of(saltMasterPrivateKeySecretPath,
                        BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes())))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, String saltMasterPrivateKeySecretPath) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn))
                .withRotationJob(() -> updateSaltMasterKeyPair(resourceCrn, RotationSecret::getSecret))
                .withRollbackJob(() -> updateSaltMasterKeyPair(resourceCrn, RotationSecret::getBackupSecret))
                .build();
    }

    private void validateAllInstancesAreReachable(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        List<String> unreachableInstances = stack.getAllAvailableInstances().stream()
                .filter(instance -> !instance.isReachable())
                .map(InstanceMetadataView::getInstanceId)
                .toList();
        if (!unreachableInstances.isEmpty()) {
            throw new SecretRotationException(String.format("Unreachable instances found: %s, salt master key rotation is not possible!", unreachableInstances));
        }
    }

    private void updateSaltMasterKeyPair(String resourceCrn, Function<RotationSecret, String> mapper) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        updateSaltMasterPublicKey(stack, mapper);
        runSaltBootstrap(stack.getId());
    }

    private void updateSaltMasterPublicKey(StackDto stack, Function<RotationSecret, String> mapper) {
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        RotationSecret saltMasterPrivateKey = uncachedSecretServiceForRotation.getRotation(saltSecurityConfig.getSaltMasterPrivateKeySecret().getSecret());
        saltSecurityConfig.setSaltMasterPublicKey(PkiUtil.calculatePublicKeyInBase64(mapper.apply(saltMasterPrivateKey)));
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
