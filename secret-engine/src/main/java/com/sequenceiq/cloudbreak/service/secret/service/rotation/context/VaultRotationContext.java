package com.sequenceiq.cloudbreak.service.secret.service.rotation.context;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

public class VaultRotationContext extends RotationContext {

    private final Map<Secret, String> newSecretMap;

    private VaultRotationContext(String resourceCrn, Map<Secret, String> newSecretMap) {
        super(resourceCrn);
        this.newSecretMap = newSecretMap;
    }

    public Map<Secret, String> getNewSecretMap() {
        return newSecretMap;
    }

    public static VaultRotationContextBuilder builder() {
        return new VaultRotationContextBuilder();
    }

    public static class VaultRotationContextBuilder {

        private String resourceCrn;

        private Map<Secret, String> newSecretMap;

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withNewSecretMap(Map<Secret, String> newSecretMap) {
            this.newSecretMap = newSecretMap;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, newSecretMap);
        }
    }
}
