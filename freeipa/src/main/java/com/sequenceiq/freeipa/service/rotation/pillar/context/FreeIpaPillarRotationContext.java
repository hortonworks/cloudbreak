package com.sequenceiq.freeipa.service.rotation.pillar.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class FreeIpaPillarRotationContext extends RotationContext {
    public FreeIpaPillarRotationContext(String envCrn) {
        super(envCrn);
    }

    public static FreeIpaPillarRotationContext.FreeipaPillarRotationContextBuilder builder() {
        return new FreeIpaPillarRotationContext.FreeipaPillarRotationContextBuilder();
    }

    public static class FreeipaPillarRotationContextBuilder {
        private String envCrn;

        public FreeipaPillarRotationContextBuilder withEnvCrn(String envCrn) {
            this.envCrn = envCrn;
            return this;
        }

        public FreeIpaPillarRotationContext build() {
            return new FreeIpaPillarRotationContext(envCrn);
        }
    }
}
