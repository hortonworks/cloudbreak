package com.sequenceiq.freeipa.service.rotation.usersyncpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;

@Component
public class FreeIpaUserSyncUserPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserSyncUserPasswordRotationContextProvider.class);

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private UserSyncBindUserService userSyncBindUserService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        LdapConfig clusterLdapConfig = ldapConfigService.find(resourceCrn, ThreadBasedUserCrnProvider.getAccountId(),
                userSyncBindUserService.createUserSyncBindUserPostfix(resourceCrn)).orElseThrow();
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(Map.of(clusterLdapConfig, Map.of(SecretMarker.LDAP_CONFIG_BIND_PWD, FreeIpaPasswordUtil.generatePassword())))
                .build();
        FreeIpaUserPasswordRotationContext freeIpaUserPasswordRotationContext = FreeIpaUserPasswordRotationContext.builder()
                .withUserName(userSyncBindUserService.getUserSyncBindUserName(resourceCrn))
                .withPasswordSecret(clusterLdapConfig.getBindPasswordSecret())
                .withResourceCrn(resourceCrn)
                .build();
        return Map.of(VAULT, vaultRotationContext, FREEIPA_USER_PASSWORD, freeIpaUserPasswordRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.FREEIPA_USERSYNC_USER_PASSWORD;
    }
}
