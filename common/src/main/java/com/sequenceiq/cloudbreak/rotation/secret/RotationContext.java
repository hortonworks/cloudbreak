package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Map;
import java.util.function.Supplier;

public class RotationContext {

    private final Map<String, String> userPasswordSecrets;

    private final Supplier<String> clientUserSecretSupplier;

    private final Supplier<String> clientPasswordSecretSupplier;

    private final String resourceCrn;

    private RotationContext(Map<String, String> userPasswordSecrets, Supplier<String> clientUserSecretSupplier,
            Supplier<String> clientPasswordSecretSupplier, String resourceCrn) {
        this.userPasswordSecrets = userPasswordSecrets;
        this.clientUserSecretSupplier = clientUserSecretSupplier;
        this.clientPasswordSecretSupplier = clientPasswordSecretSupplier;
        this.resourceCrn = resourceCrn;
    }

    public Map<String, String> getUserPasswordSecrets() {
        return userPasswordSecrets;
    }

    public Supplier<String> getClientUserSecretSupplier() {
        return clientUserSecretSupplier;
    }

    public Supplier<String> getClientPasswordSecretSupplier() {
        return clientPasswordSecretSupplier;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public static RotationContextBuilder contextBuilder() {
        return new RotationContextBuilder();
    }

    public static class RotationContextBuilder {

        private Map<String, String> userPasswordSecrets;

        private Supplier<String> clientUserSecretSupplier;

        private Supplier<String> clientPasswordSecretSupplier;

        private String resourceCrn;

        public RotationContextBuilder withUserPasswordSecrets(Map<String, String> userPasswordSecrets) {
            this.userPasswordSecrets = userPasswordSecrets;
            return this;
        }

        public RotationContextBuilder withClientUserSecretSupplier(Supplier<String> clientUserSecretSupplier) {
            this.clientUserSecretSupplier = clientUserSecretSupplier;
            return this;
        }

        public RotationContextBuilder withClientPasswordSecretSupplier(Supplier<String> clientPasswordSecretSupplier) {
            this.clientPasswordSecretSupplier = clientPasswordSecretSupplier;
            return this;
        }

        public RotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public RotationContext build() {
            return new RotationContext(userPasswordSecrets, clientUserSecretSupplier, clientPasswordSecretSupplier, resourceCrn);
        }

    }
}
