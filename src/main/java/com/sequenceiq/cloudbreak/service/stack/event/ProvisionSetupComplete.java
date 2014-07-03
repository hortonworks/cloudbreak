package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionSetupComplete extends ProvisionEvent {

    private Map<String, String> setupProperties = new HashMap<>();

    public ProvisionSetupComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

    public Map<String, String> getSetupProperties() {
        return setupProperties;
    }

    public void setSetupProperties(Map<String, String> setupProperties) {
        this.setupProperties = setupProperties;
    }

    public ProvisionSetupComplete withSetupProperty(String key, String value) {
        this.setupProperties.put(key, value);
        return this;
    }

}
