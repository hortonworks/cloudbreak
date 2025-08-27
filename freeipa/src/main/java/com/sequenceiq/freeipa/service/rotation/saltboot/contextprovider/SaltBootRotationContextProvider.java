package com.sequenceiq.freeipa.service.rotation.saltboot.contextprovider;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.LAUNCH_TEMPLATE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.saltboot.SaltBootPasswordUserDataModifier;
import com.sequenceiq.cloudbreak.rotation.secret.saltboot.SaltBootSignKeyUserDataModifier;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.saltboot.context.SaltBootConfigRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

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
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootPasswordUserDataModifier saltBootPasswordUserDataModifier;

    @Inject
    private SaltBootSignKeyUserDataModifier saltBootSignKeyUserDataModifier;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        String saltBootPasswordSecret = securityConfig.getSaltSecurityConfig().getSaltBootPasswordVaultSecret();
        String saltBootPrivateKeySecret = securityConfig.getSaltSecurityConfig().getSaltBootSignPrivateKeyVaultSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceCrn, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(CUSTOM_JOB, getUpdateDatabaseJob(resourceCrn, environmentCrn.getAccountId(), saltBootPrivateKeySecret))
                .put(SALTBOOT_CONFIG, getServiceConfigRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(USER_DATA, new UserDataRotationContext(resourceCrn,
                        List.of(Pair.of(saltBootPasswordUserDataModifier, saltBootPasswordSecret),
                                Pair.of(saltBootSignKeyUserDataModifier, saltBootPrivateKeySecret))))
                .put(LAUNCH_TEMPLATE, new RotationContext(resourceCrn))
                .build();
    }

    private CustomJobRotationContext getUpdateDatabaseJob(String environmentCrn, String accountId, String saltBootPrivateKeySecret) {
        return CustomJobRotationContext
                .builder()
                .withResourceCrn(environmentCrn)
                .withFinalizeJob(() -> deletePublicKeyFromDatabaseIfExists(environmentCrn, accountId))
                .build();
    }

    private void deletePublicKeyFromDatabaseIfExists(String resourceCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, accountId);
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        if (saltSecurityConfig.getLegacySaltBootSignPublicKey() != null) {
            saltSecurityConfig.setSaltBootSignPublicKey(null);
            securityConfigService.save(securityConfig);
        }
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.SALT_BOOT_SECRETS;
    }

    private SaltBootConfigRotationContext getServiceConfigRotationContext(Stack stack, String saltBootPasswordSecret, String saltBootPrivateKeySecret) {
        return new SaltBootConfigRotationContext(stack.getResourceCrn()) {

            @Override
            public SaltBootUpdateConfiguration getServiceUpdateConfiguration() {
                RotationSecret saltBootPassword = uncachedSecretServiceForRotation.getRotation(saltBootPasswordSecret);
                RotationSecret saltBootPrivateKey = uncachedSecretServiceForRotation.getRotation(saltBootPrivateKeySecret);
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
                        stack.getAllInstanceMetaDataList()
                                .stream()
                                .filter(InstanceMetaData::isAvailable)
                                .map(InstanceMetaData::getPrivateIp)
                                .collect(Collectors.toSet()),
                        stack.getAllInstanceMetaDataList()
                                .stream()
                                .filter(InstanceMetaData::isAvailable)
                                .map(InstanceMetaData::getDiscoveryFQDN)
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

    private VaultRotationContext getVaultRotationContext(String resourceCrn, String saltBootPasswordSecret, String saltBootPrivateKeySecret) {
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withVaultPathSecretMap(ImmutableMap.<String, String>builder()
                        .put(saltBootPasswordSecret, PasswordUtil.generatePassword())
                        .put(saltBootPrivateKeySecret, BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes()))
                        .build())
                .build();
    }
}
