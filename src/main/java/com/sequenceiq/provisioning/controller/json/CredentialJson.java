package com.sequenceiq.provisioning.controller.json;

import java.util.Map;

import com.sequenceiq.provisioning.controller.validation.ValidCredentialRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@ValidCredentialRequest
public class CredentialJson {

    private CloudPlatform cloudPlatform;
    private Map<String, String> parameters;

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
