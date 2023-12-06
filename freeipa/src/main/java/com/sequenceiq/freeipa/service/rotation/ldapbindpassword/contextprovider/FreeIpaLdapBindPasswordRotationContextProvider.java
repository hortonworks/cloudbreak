package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.ROTATE_USER_SYNC_USER;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_LDAP_BIND_PASSWORD;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.rotation.ldapbindpassword.context.FreeIpaLdapBindPasswordRotationContext;

@Component
public class FreeIpaLdapBindPasswordRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLdapBindPasswordRotationContextProvider.class);

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private UserSyncBindUserService userSyncBindUserService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContextsWithProperties(String environmentCrnAsString, Map<String, String> additionalProperties) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        LdapConfig ldapConfig = getLdapConfig(environmentCrnAsString, additionalProperties);
        String newPassword = FreeIpaPasswordUtil.generatePassword();
        Map<String, String> vaultPathMap = Maps.newHashMap();
        vaultPathMap.put(ldapConfig.getBindPasswordSecret(), newPassword);

        VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                .withResourceCrn(environmentCrnAsString)
                .withVaultPathSecretMap(vaultPathMap)
                .build();

        FreeIpaLdapBindPasswordRotationContext freeIpaLdapBindPasswordRotationContext = FreeIpaLdapBindPasswordRotationContext.builder()
                .withResourceCrn(environmentCrnAsString)
                .withBindPasswordSecret(ldapConfig.getBindPasswordSecret())
                .withClusterName(getClusterName(additionalProperties))
                .withRotateUserSyncUser(rotateUserSyncUser(additionalProperties))
                .build();

        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        result.put(VAULT, vaultRotationContext);
        result.put(FREEIPA_LDAP_BIND_PASSWORD, freeIpaLdapBindPasswordRotationContext);
        return result;
    }

    private LdapConfig getLdapConfig(String environmentCrnAsString, Map<String, String> additionalProperties) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        String clusterName = getClusterName(additionalProperties);
        boolean rotateUserSyncUser = rotateUserSyncUser(additionalProperties);
        String ldapUserName = clusterName;
        if (StringUtils.isBlank(clusterName)) {
            if (rotateUserSyncUser) {
                ldapUserName = userSyncBindUserService.createUserSyncBindUserPostfix(environmentCrnAsString);
            } else {
                throw new SecretRotationException("FreeIpa ldap bind password rotation failed, " +
                        "either CLUSTER_NAME or USER_SYNC should be set in the request.");

            }
        } else if (rotateUserSyncUser) {
            throw new SecretRotationException("For FreeIpa LDAP bind password rotation CLUSTER_NAME and USER_SYNC cannot be both set together.");
        }
        Optional<LdapConfig> clusterLdapConfig = ldapConfigService.find(environmentCrnAsString, environmentCrn.getAccountId(), ldapUserName);
        String message = String.format("FreeIpa ldap bind password rotation failed, cannot found ldap config for user: %s", ldapUserName);
        return clusterLdapConfig.orElseThrow(() -> new SecretRotationException(message));
    }

    private boolean rotateUserSyncUser(Map<String, String> additionalProperties) {
        return Optional.ofNullable(additionalProperties)
                .map(properties -> properties.get(ROTATE_USER_SYNC_USER.name()))
                .map(usersync -> "true".equalsIgnoreCase(usersync))
                .orElse(false);
    }

    private static String getClusterName(Map<String, String> additionalProperties) {
        return Optional.ofNullable(additionalProperties)
                .map(properties -> properties.get(CLUSTER_NAME.name()))
                .orElse("");
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD;
    }
}
