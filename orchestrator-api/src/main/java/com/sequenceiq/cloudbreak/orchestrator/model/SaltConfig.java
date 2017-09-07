package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.HashMap;
import java.util.Map;

public class SaltConfig {
    private final Map<String, SaltPillarProperties> servicePillarConfig;

    private final Map<String, Map<String, String>> grainsProperties;

    public SaltConfig() {
        servicePillarConfig = new HashMap<>();
        grainsProperties = new HashMap<>();
    }

    public SaltConfig(Map<String, SaltPillarProperties> servicePillarConfig) {
        this(servicePillarConfig, new HashMap<>());
    }

    public SaltConfig(Map<String, SaltPillarProperties> servicePillarConfig, Map<String, Map<String, String>> grainsProperties) {
        this.servicePillarConfig = servicePillarConfig;
        this.grainsProperties = grainsProperties;
    }

    public Map<String, SaltPillarProperties> getServicePillarConfig() {
        return servicePillarConfig;
    }

    public Map<String, Map<String, String>> getGrainsProperties() {
        return grainsProperties;
    }
}
