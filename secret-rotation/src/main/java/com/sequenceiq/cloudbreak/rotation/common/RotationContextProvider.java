package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;

public interface RotationContextProvider {

    <C extends RotationContext> Map<SecretRotationStep, C> getContexts(String resourceCrn);

    SecretType getSecret();

    default <C extends RotationContext> Set<String> getVaultSecretsForRollback(String resourceCrn, Map<SecretRotationStep, C> contexts) {
        if (getSecret().getSteps().contains(VAULT)) {
            return ((VaultRotationContext) contexts.get(VAULT)).getVaultPathSecretMap().keySet();
        } else {
            return getVaultSecretsForRollback(resourceCrn);
        }
    }

    default Set<String> getVaultSecretsForRollback(String resourceCrn) {
        return Set.of();
    }
}
