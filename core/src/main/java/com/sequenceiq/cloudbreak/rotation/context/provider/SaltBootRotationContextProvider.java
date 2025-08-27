package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootUpdateConfiguration;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.saltboot.SaltBootPasswordUserDataModifier;
import com.sequenceiq.cloudbreak.rotation.secret.saltboot.SaltBootSignKeyUserDataModifier;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class SaltBootRotationContextProvider implements RotationContextProvider {

    private static final int SALT_STATE_MAX_RETRY = 3;

    private static final String SALT_BOOT_CONFIG_TEMPLATE = "username: $USERNAME\n" +
            "password: $PASSWORD\n" +
            "signKey: |-\n" +
            "  -----BEGIN PUBLIC KEY-----\n" +
            "  $PUBLIC_KEY\n" +
            "  -----END PUBLIC KEY-----\n";

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

    @Inject
    private StackDtoService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootPasswordUserDataModifier saltBootPasswordUserDataModifier;

    @Inject
    private SaltBootSignKeyUserDataModifier saltBootSignKeyUserDataModifier;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        Secret saltBootPasswordSecret = saltSecurityConfig.getSaltBootPasswordSecret();
        Secret saltBootPrivateKeySecret = saltSecurityConfig.getSaltBootSignPrivateKeySecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(CUSTOM_JOB, getSaltPublicKeyUpdateJob(resourceCrn, saltBootPrivateKeySecret.getSecret()))
                .put(SALTBOOT_CONFIG, getSaltBootRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(USER_DATA, getUserDataRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .build();
    }

    private CustomJobRotationContext getSaltPublicKeyUpdateJob(String resourceCrn, String saltBootPrivateKeyPath) {
        return CustomJobRotationContext
                .builder()
                .withResourceCrn(resourceCrn)
                .withFinalizeJob(() -> deletePublicKeyFromDatabaseIfExists(resourceCrn))
                .build();
    }

    private void deletePublicKeyFromDatabaseIfExists(String resourceCrn) {
        StackDto stack = stackService.getByCrn(resourceCrn);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        if (saltSecurityConfig.getLegacySaltBootSignPublicKey() != null) {
            saltSecurityConfig.setSaltBootSignPublicKey(null);
            saltSecurityConfigService.save(saltSecurityConfig);
        }
    }

    private UserDataRotationContext getUserDataRotationContext(StackDto stack, Secret saltBootPasswordSecret, Secret saltBootPrivateKeySecret) {
        return new UserDataRotationContext(stack.getResourceCrn(), List.of(
                Pair.of(saltBootPasswordUserDataModifier, saltBootPasswordSecret.getSecret()),
                Pair.of(saltBootSignKeyUserDataModifier, saltBootPrivateKeySecret.getSecret())));
    }

    private SaltBootConfigRotationContext getSaltBootRotationContext(StackDto stack, Secret saltBootPasswordSecret, Secret saltBootPrivateKeySecret) {
        return new SaltBootConfigRotationContext(stack.getResourceCrn()) {

            @Override
            public SaltBootUpdateConfiguration getServiceUpdateConfiguration() {
                RotationSecret saltBootPassword = uncachedSecretServiceForRotation.getRotation(saltBootPasswordSecret.getSecret());
                RotationSecret saltBootPrivateKey = uncachedSecretServiceForRotation.getRotation(saltBootPrivateKeySecret.getSecret());
                String oldSaltBootPassword = saltBootPassword.isRotation() ? saltBootPassword.getBackupSecret() : saltBootPassword.getSecret();
                String newSaltBootPassword = saltBootPassword.getSecret();
                String oldSaltBootPrivateKey = saltBootPrivateKey.isRotation() ? saltBootPrivateKey.getBackupSecret() : saltBootPrivateKey.getSecret();
                String newSaltBootPrivateKey = saltBootPrivateKey.getSecret();
                return new SaltBootUpdateConfiguration(
                        gatewayConfigService.getPrimaryGatewayConfig(stack),
                        oldSaltBootPassword,
                        newSaltBootPassword,
                        oldSaltBootPrivateKey,
                        newSaltBootPrivateKey,
                        "/etc/salt-bootstrap",
                        GCP.equalsIgnoreCase(stack.getCloudPlatform()) ? "rotated-security-config.yml" : "security-config.yml",
                        generateSaltBootSecretConfig(newSaltBootPassword, newSaltBootPrivateKey),
                        generateSaltBootSecretConfig(oldSaltBootPassword, oldSaltBootPrivateKey),
                        stack.getAllAvailableInstances()
                                .stream()
                                .map(InstanceMetadataView::getPrivateIp)
                                .collect(Collectors.toSet()),
                        stack.getAllAvailableInstances()
                                .stream()
                                .map(InstanceMetadataView::getDiscoveryFQDN)
                                .collect(Collectors.toSet()),
                        List.of("saltboot.restart-rotated-saltboot"),
                        SALT_STATE_MAX_RETRY,
                        exitCriteriaProvider.get(stack));
            }
        };
    }

    private String generateSaltBootSecretConfig(String password, String privateKey) {
        return SALT_BOOT_CONFIG_TEMPLATE
                .replace("$USERNAME", "cbadmin")
                .replace("$PASSWORD", password)
                .replace("$PUBLIC_KEY", BASE64.encode(PkiUtil.getPublicKeyDer(new String(BASE64.decode(privateKey)))));
    }

    private VaultRotationContext getVaultRotationContext(String resourceCrn, Secret saltBootPasswordSecret, Secret saltBootPrivateKeySecret) {
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(ImmutableMap.<String, String>builder()
                        .put(saltBootPasswordSecret.getSecret(), PasswordUtil.generatePassword())
                        .put(saltBootPrivateKeySecret.getSecret(),
                                BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes()))
                        .build())
                .build();
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.SALT_BOOT_SECRETS;
    }
}
