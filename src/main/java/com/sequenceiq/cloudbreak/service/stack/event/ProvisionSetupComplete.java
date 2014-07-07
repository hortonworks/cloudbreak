package com.sequenceiq.cloudbreak.service.stack.event;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionSetupComplete extends ProvisionEvent {

    private Map<String, Object> setupProperties = new HashMap<>();
    private Map<String, String> userDataParams = new HashMap<>();

    public ProvisionSetupComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

    public Map<String, Object> getSetupProperties() {
        return setupProperties;
    }

    public void setSetupProperties(Map<String, Object> setupProperties) {
        this.setupProperties = setupProperties;
    }

    public Map<String, String> getUserDataParams() {
        return userDataParams;
    }

    public void setUserDataParams(Map<String, String> userDataParams) {
        this.userDataParams = userDataParams;
    }

    public ProvisionSetupComplete withSetupProperty(String key, Object value) {
        this.setupProperties.put(key, value);
        return this;
    }

    public ProvisionSetupComplete withUserDataParam(String key, String value) {
        this.userDataParams.put(key, value);
        return this;
    }

}
