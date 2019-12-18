package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaltConfig {
    private final Map<String, SaltPillarProperties> servicePillarConfig;

    private final List<GrainProperties> grainsProperties;

    public SaltConfig() {
        servicePillarConfig = new HashMap<>();
        grainsProperties = new ArrayList<>();
    }

    public SaltConfig(Map<String, SaltPillarProperties> servicePillarConfig) {
        this(servicePillarConfig, new ArrayList<>());
    }

    public SaltConfig(Map<String, SaltPillarProperties> servicePillarConfig, List<GrainProperties> grainsProperties) {
        this.servicePillarConfig = servicePillarConfig;
        this.grainsProperties = grainsProperties;
    }

    public Map<String, SaltPillarProperties> getServicePillarConfig() {
        return servicePillarConfig;
    }

    public List<GrainProperties> getGrainsProperties() {
        return grainsProperties;
    }
}
