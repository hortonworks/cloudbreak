package com.sequenceiq.cloudbreak.rotation.context;

import java.util.function.Supplier;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class ClusterProxyUpdateConfigRotationContext extends RotationContext {

    private final Supplier<String> knoxSecretPath;

    protected ClusterProxyUpdateConfigRotationContext(String resourceCrn, Supplier<String> knoxSecretPath) {
        super(resourceCrn);
        this.knoxSecretPath = knoxSecretPath;
    }

    public Supplier<String> getKnoxSecretPath() {
        return knoxSecretPath;
    }

    public static ClusterProxyUpdateConfigRotationContextBuilder builder() {
        return new ClusterProxyUpdateConfigRotationContextBuilder();
    }

    public static class ClusterProxyUpdateConfigRotationContextBuilder {

        private String resourceCrn;

        private Supplier<String> knoxSecretPath;

        public ClusterProxyUpdateConfigRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContextBuilder withKnoxSecretPath(Supplier<String> knoxSecretPath) {
            this.knoxSecretPath = knoxSecretPath;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContext build() {
            return new ClusterProxyUpdateConfigRotationContext(resourceCrn, knoxSecretPath);
        }

    }
}
