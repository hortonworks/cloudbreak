package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class SaltMasterKeyPairRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltMasterKeyPairRotationContextProvider.class);

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.SALT_MASTER_KEY_PAIR;
    }

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        generateAndStoreSaltMasterPrivateKeyIfMissing(saltSecurityConfig);
        String saltMasterPrivateKeySecretPath = saltSecurityConfig.getSaltMasterPrivateKeySecret().getSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltSecurityConfig))
                .put(CUSTOM_JOB, getCustomJobRotationContext(resourceCrn, stack.getId()))
                .build();
    }

    private void generateAndStoreSaltMasterPrivateKeyIfMissing(SaltSecurityConfig saltSecurityConfig) {
        if (StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKey())) {
            LOGGER.info("Salt master private key is missing from the database and Vault, generate and store a new private key.");
            saltSecurityConfig.setSaltMasterPrivateKey(PkiUtil.generatePemPrivateKeyInBase64());
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private VaultRotationContext getVaultRotationContext(String resourceCrn, SaltSecurityConfig saltSecurityConfig) {
        Map<String, String> newSecretMap = Map.of(saltSecurityConfig.getSaltMasterPrivateKeySecret().getSecret(), PkiUtil.generatePemPrivateKeyInBase64());
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(newSecretMap)
                .withEntitySaverList(List.of(() -> saltSecurityConfigService.save(saltSecurityConfig)))
                .withEntitySecretFieldUpdaterMap(Map.of(saltSecurityConfig.getSaltMasterPrivateKeySecret().getSecret(),
                        vaultSecretJson -> saltSecurityConfig.setSaltMasterPrivateKeySecret(new SecretProxy(vaultSecretJson))))
                .build();
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, Long stackId) {
        return CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withPreValidateJob(() -> validateAllInstancesAreReachable(resourceCrn))
                .withRotationJob(() -> updateSaltMasterKeyPairOnCluster(stackId))
                .withRollbackJob(() -> updateSaltMasterKeyPairOnCluster(stackId))
                .withPostValidateJob(() -> validateAllInstancesAreReachable(resourceCrn))
                .withFinalizeJob(() -> deletePublicKeyFromDatabaseIfExists(resourceCrn))
                .build();
    }

    private void validateAllInstancesAreReachable(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        secretRotationSaltService.validateSalt(stack);
    }

    private void deletePublicKeyFromDatabaseIfExists(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        if (saltSecurityConfig.getLegacySaltMasterPublicKey() != null) {
            saltSecurityConfig.setSaltMasterPublicKey(null);
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private void updateSaltMasterKeyPairOnCluster(Long stackId) {
        try {
            LOGGER.info("Bootstrap machines to upload new salt master key pair based on the rotation phase");
            clusterBootstrapper.bootstrapMachines(stackId, true);
            LOGGER.info("Bootstrapping and restarting salt finished, salt master key pair uploaded.");
        } catch (CloudbreakException e) {
            LOGGER.warn("Bootstrapping and restarting salt failed", e);
            throw new SecretRotationException(e.getMessage(), e);
        }
    }
}
