package com.sequenceiq.freeipa.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;
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
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.api.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.rotation.context.saltboot.SaltBootConfigRotationContext;
import com.sequenceiq.freeipa.service.rotation.context.saltboot.SaltBootUpdateConfiguration;
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
    private SecretService secretService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootPasswordUserDataModifier saltBootPasswordUserDataModifier;

    @Inject
    private SaltBootSignKeyUserDataModifier saltBootSignKeyUserDataModifier;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceId) {
        Crn environmentCrn = Crn.safeFromString(resourceId);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceId, environmentCrn.getAccountId());
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        String saltBootPasswordSecret = securityConfig.getSaltSecurityConfig().getSaltBootPasswordVaultSecret();
        String saltBootPrivateKeySecret = securityConfig.getSaltSecurityConfig().getSaltBootSignPrivateKeyVaultSecret();
        return ImmutableMap.<SecretRotationStep, RotationContext>builder()
                .put(VAULT, getVaultRotationContext(resourceId, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(CUSTOM_JOB, getUpdateDatabaseJob(resourceId, environmentCrn.getAccountId(), saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(SALTBOOT_CONFIG, getServiceConfigRotationContext(stack, saltBootPasswordSecret, saltBootPrivateKeySecret))
                .put(USER_DATA, new UserDataRotationContext(resourceId,
                        List.of(Pair.of(saltBootPasswordUserDataModifier, saltBootPasswordSecret),
                                Pair.of(saltBootSignKeyUserDataModifier, saltBootPrivateKeySecret))))
                .build();
    }

    private CustomJobRotationContext getUpdateDatabaseJob(String environmentCrn, String accountId,
            String saltBootPasswordSecret, String saltBootPrivateKeySecret) {
        return CustomJobRotationContext
                .builder()
                .withResourceCrn(environmentCrn)
                .withRotationJob(() ->
                        updateSaltSecurityConfigColumns(environmentCrn, accountId, saltBootPasswordSecret, saltBootPrivateKeySecret,
                                RotationSecret::getSecret))
                .withRollbackJob(() ->
                        updateSaltSecurityConfigColumns(environmentCrn, accountId, saltBootPasswordSecret, saltBootPrivateKeySecret,
                                RotationSecret::getBackupSecret))
                .build();
    }

    private void updateSaltSecurityConfigColumns(String resourceId, String accountId, String saltBootPasswordSecret, String saltBootPrivateKeySecret,
            Function<RotationSecret, String> mapper) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceId, accountId);
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        RotationSecret saltBootPassword = secretService.getRotation(saltBootPasswordSecret);
        securityConfig.getSaltSecurityConfig().setSaltBootPassword(mapper.apply(saltBootPassword));
        RotationSecret saltBootPrivateKey = secretService.getRotation(saltBootPrivateKeySecret);
        String privateKey = mapper.apply(saltBootPrivateKey);
        securityConfig.getSaltSecurityConfig().setSaltBootSignPrivateKey(privateKey);
        securityConfig.getSaltSecurityConfig().setSaltBootSignPublicKey(calcSaltBootPublicKey(privateKey));
        securityConfigService.save(securityConfig);
    }

    private String calcSaltBootPublicKey(String privateKey) {
        PublicKey publicKey = PkiUtil.getPublicKey(new String(BASE64.decode(privateKey)));
        String openSshFormatPublicKey = PkiUtil.convertOpenSshPublicKey(publicKey);
        return BASE64.encode(openSshFormatPublicKey.getBytes());
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.SALT_BOOT_SECRETS;
    }

    private SaltBootConfigRotationContext getServiceConfigRotationContext(Stack stack, String saltBootPasswordSecret, String saltBootPrivateKeySecret) {
        return new SaltBootConfigRotationContext(stack.getResourceCrn()) {

            @Override
            public SaltBootUpdateConfiguration getServiceUpdateConfiguration() {
                RotationSecret saltBootPassword = secretService.getRotation(saltBootPasswordSecret);
                RotationSecret saltBootPrivateKey = secretService.getRotation(saltBootPrivateKeySecret);
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
                        "security-config.yml",
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
                        List.of("saltboot.stop-saltboot", "saltboot.start-saltboot"),
                        SALT_STATE_MAX_RETRY,
                        StackBasedExitCriteriaModel.nonCancellableModel()
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
