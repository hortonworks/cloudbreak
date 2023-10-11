package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class ClusterProxyReRegisterRotationContext extends RotationContext {

    protected ClusterProxyReRegisterRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public static ClusterProxyReRegisterRotationContextBuilder builder() {
        return new ClusterProxyReRegisterRotationContextBuilder();
    }

    public static class ClusterProxyReRegisterRotationContextBuilder {

        private String resourceCrn;

        public ClusterProxyReRegisterRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ClusterProxyReRegisterRotationContext build() {
            return new ClusterProxyReRegisterRotationContext(resourceCrn);
        }

    }
}
