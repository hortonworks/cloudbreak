package com.sequenceiq.cloudbreak.rotation.saltboot;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SERVICE_CONFIG;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.rotation.ServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.orchestrator.rotation.ServiceUpdateConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.salt.rotation.SaltBootPasswordUserDataModifier;
import com.sequenceiq.cloudbreak.orchestrator.salt.rotation.SaltBootSignKeyUserDataModifier;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
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
    private SecretService secretService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootPasswordUserDataModifier saltBootPasswordUserDataModifier;

    @Inject
    private SaltBootSignKeyUserDataModifier saltBootSignKeyUserDataModifier;

    @Inject
    private SaltSecurityConfigService securityConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceId) {
        StackDto stack = stackService.getByCrn(resourceId);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        Secret saltBootPasswordSecret = saltSecurityConfig.getSaltBootPasswordSecret();
        Secret saltBootPrivateKeySecret = saltSecurityConfig.getSaltBootSignPrivateKeySecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceId, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(CUSTOM_JOB, getSaltPublicKeyUpdateJob(resourceId, saltBootPrivateKeySecret.getSecret()))
                .put(SERVICE_CONFIG, getSaltBootRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(USER_DATA, getUserDataRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .build();
    }

    private CustomJobRotationContext getSaltPublicKeyUpdateJob(String resourceId, String saltBootPrivateKeyPath) {
        return CustomJobRotationContext
                .builder()
                .withResourceCrn(resourceId)
                .withRotationJob(() -> updateSaltBootPublicKey(resourceId, saltBootPrivateKeyPath, RotationSecret::getSecret))
                .withRollbackJob(() -> updateSaltBootPublicKey(resourceId, saltBootPrivateKeyPath, RotationSecret::getBackupSecret))
                .build();
    }

    private void updateSaltBootPublicKey(String resourceId, String saltBootPrivateKeySecret, Function<RotationSecret, String> mapper) {
        StackDto stack = stackService.getByCrn(resourceId);
        RotationSecret saltBootPrivateKey = secretService.getRotation(saltBootPrivateKeySecret);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        saltSecurityConfig.setSaltBootSignPublicKey(calcSaltBootPublicKey(mapper.apply(saltBootPrivateKey)));
        securityConfigService.save(saltSecurityConfig);
    }

    private String calcSaltBootPublicKey(String privateKey) {
        PublicKey publicKey = PkiUtil.getPublicKey(new String(BASE64.decode(privateKey)));
        String openSshFormatPublicKey = PkiUtil.convertOpenSshPublicKey(publicKey);
        return BASE64.encode(openSshFormatPublicKey.getBytes());
    }

    private UserDataRotationContext getUserDataRotationContext(StackDto stack, Secret saltBootPasswordSecret, Secret saltBootPrivateKeySecret) {
        return new UserDataRotationContext(stack.getResourceCrn(), List.of(
                Pair.of(saltBootPasswordUserDataModifier, saltBootPasswordSecret.getSecret()),
                Pair.of(saltBootSignKeyUserDataModifier, saltBootPrivateKeySecret.getSecret())));
    }

    private ServiceConfigRotationContext getSaltBootRotationContext(StackDto stack, Secret saltBootPasswordSecret, Secret saltBootPrivateKeySecret) {
        return new ServiceConfigRotationContext(stack.getResourceCrn()) {

            @Override
            public ServiceUpdateConfiguration getServiceUpdateConfiguration() {
                RotationSecret saltBootPassword = secretService.getRotation(saltBootPasswordSecret.getSecret());
                RotationSecret saltBootPrivateKey = secretService.getRotation(saltBootPrivateKeySecret.getSecret());
                String oldSaltBootPassword = saltBootPassword.isRotation() ? saltBootPassword.getBackupSecret() : saltBootPassword.getSecret();
                String newSaltBootPassword = saltBootPassword.getSecret();
                String oldSaltBootPrivateKey = saltBootPrivateKey.isRotation() ? saltBootPrivateKey.getBackupSecret() : saltBootPrivateKey.getSecret();
                String newSaltBootPrivateKey = saltBootPrivateKey.getSecret();
                return new ServiceUpdateConfiguration(
                        gatewayConfigService.getPrimaryGatewayConfig(stack),
                        oldSaltBootPassword,
                        newSaltBootPassword,
                        oldSaltBootPrivateKey,
                        newSaltBootPrivateKey,
                        "/etc/salt-bootstrap",
                        "security-config.yml",
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
                        List.of("saltboot.stop-saltboot", "saltboot.start-saltboot"),
                        SALT_STATE_MAX_RETRY,
                        new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId())
                );
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
