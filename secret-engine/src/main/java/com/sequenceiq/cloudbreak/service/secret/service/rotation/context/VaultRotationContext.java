package com.sequenceiq.cloudbreak.service.secret.service.rotation.context;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretGenerator;

public class VaultRotationContext extends RotationContext {

    private Map<String, Class<? extends SecretGenerator>> secretUpdateSupplierMap;

    private VaultRotationContext(String resourceCrn, Map<String, Class<? extends SecretGenerator>> secretUpdateSupplierMap) {
        super(resourceCrn);
        this.secretUpdateSupplierMap = secretUpdateSupplierMap;
    }

    public Map<String, Class<? extends SecretGenerator>> getSecretUpdateSupplierMap() {
        return secretUpdateSupplierMap;
    }

    public static VaultRotationContextBuilder builder() {
        return new VaultRotationContextBuilder();
    }

    public static class VaultRotationContextBuilder {

        private String resourceCrn;

        private Map<String, Class<? extends SecretGenerator>> secretUpdateSupplierMap;

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withSecretUpdateSupplierMap(Map<String, Class<? extends SecretGenerator>> secretUpdateSupplierMap) {
            this.secretUpdateSupplierMap = secretUpdateSupplierMap;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, secretUpdateSupplierMap);
        }
    }
}
