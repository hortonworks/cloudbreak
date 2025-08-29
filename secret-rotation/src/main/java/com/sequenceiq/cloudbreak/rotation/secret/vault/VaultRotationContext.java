package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class VaultRotationContext extends RotationContext {

    private final Map<String, Consumer<String>> entitySecretFieldUpdaterMap;

    private final Map<String, String> newSecretMap;

    private final List<Runnable> entitySaverList;

    private VaultRotationContext(String resourceCrn,
            Map<String, Consumer<String>> entitySecretFieldUpdaterMap,
            Map<String, String> newSecretMap,
            List<Runnable> entitySaverList) {
        super(resourceCrn);
        this.entitySecretFieldUpdaterMap = MapUtils.emptyIfNull(entitySecretFieldUpdaterMap);
        this.newSecretMap = MapUtils.emptyIfNull(newSecretMap);
        this.entitySaverList = ListUtils.emptyIfNull(entitySaverList);
    }

    public Map<String, Consumer<String>> getEntitySecretFieldUpdaterMap() {
        return entitySecretFieldUpdaterMap;
    }

    public Map<String, String> getNewSecretMap() {
        return newSecretMap;
    }

    public List<Runnable> getEntitySaverList() {
        return entitySaverList;
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

        private Map<String, Consumer<String>> entitySecretFieldUpdaterMap;

        private Map<String, String> newSecretMap;

        private List<Runnable> entitySaverList;

        public VaultRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public VaultRotationContextBuilder withEntitySecretFieldUpdaterMap(Map<String, Consumer<String>> entitySecretFieldUpdaterMap) {
            this.entitySecretFieldUpdaterMap = entitySecretFieldUpdaterMap;
            return this;
        }

        public VaultRotationContextBuilder withNewSecretMap(Map<String, String> newSecretMap) {
            this.newSecretMap = newSecretMap;
            return this;
        }

        public VaultRotationContextBuilder withEntitySaverList(List<Runnable> entitySaverList) {
            this.entitySaverList = entitySaverList;
            return this;
        }

        public VaultRotationContext build() {
            return new VaultRotationContext(resourceCrn, entitySecretFieldUpdaterMap, newSecretMap, entitySaverList);
        }

        @Override
        public final String toString() {
            return "VaultRotationContextBuilder{" +
                    "resourceCrn='" + resourceCrn + '\'' +
                    '}';
        }
    }
}
