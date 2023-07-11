package com.sequenceiq.cloudbreak.rotation.context;

import java.util.Map;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class SaltPillarRotationContext extends RotationContext {

    private final Function<StackDto, Map<String, SaltPillarProperties>> servicePillarGenerator;

    public SaltPillarRotationContext(String resourceCrn, Function<StackDto, Map<String, SaltPillarProperties>> servicePillarGenerator) {
        super(resourceCrn);
        this.servicePillarGenerator = servicePillarGenerator;
    }

    public Function<StackDto, Map<String, SaltPillarProperties>> getServicePillarGenerator() {
        return servicePillarGenerator;
    }
}
