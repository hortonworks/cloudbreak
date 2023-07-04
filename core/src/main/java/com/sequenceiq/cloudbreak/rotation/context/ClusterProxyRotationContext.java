package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class ClusterProxyRotationContext extends RotationContext {

    protected ClusterProxyRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public static ClusterProxyRotationContextBuilder builder() {
        return new ClusterProxyRotationContextBuilder();
    }

    public static class ClusterProxyRotationContextBuilder {

        private String resourceCrn;

        public ClusterProxyRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ClusterProxyRotationContext build() {
            return new ClusterProxyRotationContext(resourceCrn);
        }

    }
}
