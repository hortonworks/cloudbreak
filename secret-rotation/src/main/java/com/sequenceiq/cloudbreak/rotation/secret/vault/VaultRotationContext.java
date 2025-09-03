package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;

public class VaultRotationContext extends RotationContext {

    private final Map<? extends Object, Map<SecretMarker, String>> newSecretMap;

    private VaultRotationContext(String resourceCrn,
            Map<? extends Object, Map<SecretMarker, String>> newSecretMap) {
        super(resourceCrn);
        this.newSecretMap = MapUtils.emptyIfNull(newSecretMap);
    }

    public Map<? extends Object, Map<SecretMarker, String>> getNewSecretMap() {
        return newSecretMap;
    }

    public static VaultRotationContextBuilder builder() {
        return new VaultRotationContextBuilder();
    }

    @Override
    public final String toString() {
        return "VaultRotationContext{" +
                ", resourceCrn='" + getResourceCrn() +
                '}';
    }

    public static class VaultRotationContextBuilder {

        private String resourceCrn;

        private Map<? extends Object, Map<SecretMarker, String>> newSecretMap;

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withNewSecretMap(Map<? extends Object, Map<SecretMarker, String>> newSecretMap) {
            this.newSecretMap = newSecretMap;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, newSecretMap);
        }

        @Override
        public final String toString() {
            return "VaultRotationContextBuilder{" +
                    "resourceCrn='" + resourceCrn + '\'' +
                    '}';
        }
    }
}
