package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class VaultRotationContext extends RotationContext {

    private final Map<String, String> vaultPathSecretMap;

    private VaultRotationContext(String resourceCrn, Map<String, String> vaultPathSecretMap) {
        super(resourceCrn);
        this.vaultPathSecretMap = vaultPathSecretMap;
    }

    public Map<String, String> getVaultPathSecretMap() {
        return vaultPathSecretMap;
    }

    public static VaultRotationContextBuilder builder() {
        return new VaultRotationContextBuilder();
    }

    @Override
    public final String toString() {
        return "VaultRotationContext{" +
                "vaultPathSecretMap=" + vaultPathSecretMap.keySet() +
                ", resourceCrn='" + getResourceCrn() +
                '}';
    }

    public static class VaultRotationContextBuilder {

        private String resourceCrn;

        private Map<String, String> vaultPathSecretMap;

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withVaultPathSecretMap(Map<String, String> vaultPathSecretMap) {
            this.vaultPathSecretMap = vaultPathSecretMap;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, vaultPathSecretMap);
        }

        @Override
        public final String toString() {
            return "VaultRotationContextBuilder{" +
                    "resourceCrn='" + resourceCrn + '\'' +
                    ", vaultPathSecretMap=" + vaultPathSecretMap.keySet() +
                    '}';
        }
    }
}
