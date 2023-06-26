package com.sequenceiq.freeipa.service.rotation.pillar.context;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public class FreeipaPillarRotationContext extends RotationContext {
    public FreeipaPillarRotationContext(String resourceCrn) {
        super(resourceCrn);
    }

    public static FreeipaPillarRotationContext.FreeipaPillarRotationContextBuilder builder() {
        return new FreeipaPillarRotationContext.FreeipaPillarRotationContextBuilder();
    }

    public static class FreeipaPillarRotationContextBuilder {
        private String resourceCrn;

        public FreeipaPillarRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public FreeipaPillarRotationContext build() {
            return new FreeipaPillarRotationContext(resourceCrn);
        }
    }
}
