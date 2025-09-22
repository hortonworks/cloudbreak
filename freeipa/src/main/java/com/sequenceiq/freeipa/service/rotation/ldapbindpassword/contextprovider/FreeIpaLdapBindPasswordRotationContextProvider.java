package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;

@Component
public class FreeIpaLdapBindPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLdapBindPasswordRotationContextProvider.class);

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapBindUserNameProvider userNameProvider;

    @Override
    public Map<SecretRotationStep, RotationContext> getContextsWithProperties(String resourceCrn, Map<String, String> additionalProperties) {
        if (!additionalProperties.containsKey(CLUSTER_NAME.name())) {
            throw new SecretRotationException("There is no cluster name provided for the rotation, thus related LDAP config cannot be looked up.");
        }
        String clusterName = additionalProperties.get(CLUSTER_NAME.name());
        String ldapUserName = userNameProvider.createBindUserName(clusterName);
        LdapConfig clusterLdapConfig = ldapConfigService.find(resourceCrn, ThreadBasedUserCrnProvider.getAccountId(), clusterName).orElseThrow();
        if (!StringUtils.equals(clusterName, clusterLdapConfig.getClusterName())) {
            throw new SecretRotationException("There is no LDAP config for the given cluster name.");
        }
        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(Map.of(clusterLdapConfig, Map.of(SecretMarker.LDAP_CONFIG_BIND_PWD, FreeIpaPasswordUtil.generatePassword())))
                .build();
        FreeIpaUserPasswordRotationContext freeIpaUserPasswordRotationContext = FreeIpaUserPasswordRotationContext.builder()
                .withUserName(ldapUserName)
                .withPasswordSecret(clusterLdapConfig.getBindPasswordSecret())
                .withResourceCrn(resourceCrn)
                .build();
        return Map.of(VAULT, vaultRotationContext, FREEIPA_USER_PASSWORD, freeIpaUserPasswordRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD;
    }
}
