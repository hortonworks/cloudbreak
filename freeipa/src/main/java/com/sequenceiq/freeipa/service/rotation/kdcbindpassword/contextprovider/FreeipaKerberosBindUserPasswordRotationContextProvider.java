package com.sequenceiq.freeipa.service.rotation.kdcbindpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.v1.KerberosConfigV1Service;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;

@Component
public class FreeipaKerberosBindUserPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Inject
    private SecretService secretService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContextsWithProperties(String resourceCrn, Map<String, String> additionalProperties) {
        try {
            if (!additionalProperties.containsKey(CLUSTER_NAME.name())) {
                throw new SecretRotationException("There is no cluster name provided for the rotation, thus related kerberos config cannot be looked up.");
            }
            String clusterName = additionalProperties.get(CLUSTER_NAME.name());
            KerberosConfig kerberosConfig = kerberosConfigV1Service.getForCluster(resourceCrn, ThreadBasedUserCrnProvider.getAccountId(), clusterName);
            if (!StringUtils.equals(clusterName, kerberosConfig.getClusterName())) {
                throw new SecretRotationException("There is no kerberos config for the given cluster name.");
            }
            VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                    .withResourceCrn(resourceCrn)
                    .withVaultPathSecretMap(Map.of(kerberosConfig.getPasswordSecret(), FreeIpaPasswordUtil.generatePassword()))
                    .build();
            FreeIpaUserPasswordRotationContext freeIpaUserPasswordRotationContext = FreeIpaUserPasswordRotationContext.builder()
                    .withUserName(secretService.get(kerberosConfig.getPrincipalSecret()))
                    .withPasswordSecret(kerberosConfig.getPasswordSecret())
                    .withResourceCrn(resourceCrn)
                    .build();
            return Map.of(VAULT, vaultRotationContext, FREEIPA_USER_PASSWORD, freeIpaUserPasswordRotationContext);
        } catch (Exception e) {
            throw new SecretRotationException(String.format("Failed to generate context for %s: ", getSecret()), e);
        }
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER;
    }
}
