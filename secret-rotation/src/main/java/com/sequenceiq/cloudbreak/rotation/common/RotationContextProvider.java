package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationReflectionUtil;

public interface RotationContextProvider {

    default Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        throw new NotImplementedException("getContexts should be implemented");
    }

    default Map<SecretRotationStep, ? extends RotationContext> getContextsWithProperties(String resourceCrn, Map<String, String> additionalProperties) {
        return getContexts(resourceCrn);
    }

    SecretType getSecret();

    default Set<String> getVaultSecretsForRollback(String resourceCrn, Map<SecretRotationStep, ? extends RotationContext> contexts) {
        if (getSecret().getSteps().contains(VAULT)) {
            return MapUtils.emptyIfNull(((VaultRotationContext) contexts.get(VAULT)).getNewSecretMap()).entrySet()
                    .stream()
                    .map(entry -> MapUtils.emptyIfNull(entry.getValue()).keySet()
                            .stream()
                            .map(marker -> VaultRotationReflectionUtil.getVaultSecretJson(entry.getKey(), marker))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet()))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        } else {
            return getVaultSecretsForRollback(resourceCrn);
        }
    }

    default Set<String> getVaultSecretsForRollback(String resourceCrn) {
        return Set.of();
    }
}
