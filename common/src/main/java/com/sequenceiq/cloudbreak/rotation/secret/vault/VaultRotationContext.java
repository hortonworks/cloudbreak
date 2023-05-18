package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretGenerator;

public class VaultRotationContext extends RotationContext {

    private final Map<String, Class<? extends SecretGenerator>> secretGenerators;

    private final Map<Class<? extends SecretGenerator>, Map<String, Object>> secretGeneratorArguments;

    private VaultRotationContext(String resourceCrn,
            Map<String, Class<? extends SecretGenerator>> secretGenerators,
            Map<Class<? extends SecretGenerator>, Map<String, Object>> secretGeneratorArguments) {
        super(resourceCrn);
        this.secretGenerators = secretGenerators;
        this.secretGeneratorArguments = secretGeneratorArguments;
    }

    public Map<String, Class<? extends SecretGenerator>> getSecretGenerators() {
        return secretGenerators;
    }

    public Map<Class<? extends SecretGenerator>, Map<String, Object>> getSecretGeneratorArguments() {
        return secretGeneratorArguments;
    }

    public static VaultRotationContextBuilder builder() {
        return new VaultRotationContextBuilder();
    }

    public static class VaultRotationContextBuilder {

        private String resourceCrn;

        private Map<String, Class<? extends SecretGenerator>> secretGenerators;

        private Map<Class<? extends SecretGenerator>, Map<String, Object>> secretGeneratorArguments = Map.of();

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withSecretGenerators(Map<String, Class<? extends SecretGenerator>> secretGenerators) {
            this.secretGenerators = secretGenerators;
            return this;
        }

        public VaultRotationContextBuilder withSecretGeneratorArguments(Map<Class<? extends SecretGenerator>, Map<String, Object>> secretGeneratorArguments) {
            this.secretGeneratorArguments = secretGeneratorArguments;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, secretGenerators, secretGeneratorArguments);
        }
    }
}
