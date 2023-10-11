package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class ClusterProxyUpdateConfigRotationContext extends RotationContext {

    private final String knoxSecretPath;

    protected ClusterProxyUpdateConfigRotationContext(String resourceCrn, String knoxSecretPath) {
        super(resourceCrn);
        this.knoxSecretPath = knoxSecretPath;
    }

    public String getKnoxSecretPath() {
        return knoxSecretPath;
    }

    public static ClusterProxyUpdateConfigRotationContextBuilder builder() {
        return new ClusterProxyUpdateConfigRotationContextBuilder();
    }

    public static class ClusterProxyUpdateConfigRotationContextBuilder {

        private String resourceCrn;

        private String knoxSecretPath;

        public ClusterProxyUpdateConfigRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContextBuilder withKnoxSecretPath(String knoxSecretPath) {
            this.knoxSecretPath = knoxSecretPath;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContext build() {
            return new ClusterProxyUpdateConfigRotationContext(resourceCrn, knoxSecretPath);
        }

    }
}
