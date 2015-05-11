package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionSetupComplete extends ProvisionEvent {

    private Map<String, Object> setupProperties = new HashMap<>();

    public ProvisionSetupComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

    public Map<String, Object> getSetupProperties() {
        return setupProperties;
    }

    public void setSetupProperties(Map<String, Object> setupProperties) {
        this.setupProperties = setupProperties;
    }

    public ProvisionSetupComplete withSetupProperty(String key, Object value) {
        this.setupProperties.put(key, value);
        return this;
    }

    public ProvisionSetupComplete withSetupProperties(Map<String, Object> properties) {
        this.setupProperties = properties;
        return this;
    }
}
