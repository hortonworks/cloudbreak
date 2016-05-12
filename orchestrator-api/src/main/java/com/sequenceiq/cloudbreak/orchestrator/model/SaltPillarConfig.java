package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.HashMap;
import java.util.Map;

public class SaltPillarConfig {

    private Map<String, SaltPillarProperties> servicePillarConfig;

    public SaltPillarConfig() {
        servicePillarConfig = new HashMap<>();
    }

    public SaltPillarConfig(Map<String, SaltPillarProperties> servicePillarConfig) {
        this.servicePillarConfig = servicePillarConfig;
    }

    public Map<String, SaltPillarProperties> getServicePillarConfig() {
        return servicePillarConfig;
    }

    public void setServicePillarConfig(Map<String, SaltPillarProperties> servicePillarConfig) {
        this.servicePillarConfig = servicePillarConfig;
    }
}
