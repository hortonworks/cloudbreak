package com.sequenceiq.freeipa.service.rotation.kdcbindpassword.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static com.sequenceiq.freeipa.rotation.FreeIpaRotationAdditionalParameters.CLUSTER_NAME;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_USER_PASSWORD;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.kerberos.v1.KerberosConfigV1Service;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;

@Component
public class FreeipaKerberosBindUserPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

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
            Map<String, String> newSecretMap = Map.of(kerberosConfig.getPasswordSecret(), FreeIpaPasswordUtil.generatePassword());
            Map<String, Consumer<String>> secretUpdaterMap = Map.of(kerberosConfig.getPasswordSecret(),
                    vaultSecretJson -> kerberosConfig.setPasswordSecret(new SecretProxy(vaultSecretJson)));
            VaultRotationContext vaultRotationContext = VaultRotationContext.builder()
                    .withResourceCrn(resourceCrn)
                    .withNewSecretMap(newSecretMap)
                    .withEntitySaverList(List.of(() -> kerberosConfigService.save(kerberosConfig)))
                    .withEntitySecretFieldUpdaterMap(secretUpdaterMap)
                    .build();
            FreeIpaUserPasswordRotationContext freeIpaUserPasswordRotationContext = FreeIpaUserPasswordRotationContext.builder()
                    .withUserName(uncachedSecretServiceForRotation.get(kerberosConfig.getPrincipalSecret()))
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
