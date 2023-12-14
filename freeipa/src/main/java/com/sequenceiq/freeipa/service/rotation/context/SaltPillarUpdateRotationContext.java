package com.sequenceiq.freeipa.service.rotation.context;

import java.util.Map;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.freeipa.entity.Stack;

public class SaltPillarUpdateRotationContext extends RotationContext {

    private final Function<Stack, Map<String, SaltPillarProperties>> servicePillarGenerator;

    protected SaltPillarUpdateRotationContext(String environmentCrn, Function<Stack, Map<String, SaltPillarProperties>> servicePillarGenerator) {
        super(environmentCrn);
        this.servicePillarGenerator = servicePillarGenerator;
    }

    public Function<Stack, Map<String, SaltPillarProperties>> getServicePillarGenerator() {
        return servicePillarGenerator;
    }

    public static SaltPillarUpdateRotationContext.FreeipaPillarRotationContextBuilder builder() {
        return new SaltPillarUpdateRotationContext.FreeipaPillarRotationContextBuilder();
    }

    public static class FreeipaPillarRotationContextBuilder {

        private String environmentCrn;

        private Function<Stack, Map<String, SaltPillarProperties>> servicePillarGenerator;

        public FreeipaPillarRotationContextBuilder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public FreeipaPillarRotationContextBuilder withServicePillarGenerator(Function<Stack, Map<String, SaltPillarProperties>> servicePillarGenerator) {
            this.servicePillarGenerator = servicePillarGenerator;
            return this;
        }

        public SaltPillarUpdateRotationContext build() {
            return new SaltPillarUpdateRotationContext(environmentCrn, servicePillarGenerator);
        }
    }
}
