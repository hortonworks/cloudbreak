package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Map;

public class OrchestrationCredential {

    private String apiEndpoint;
    private Map<String, Object> properties;

    public OrchestrationCredential(String apiEndpoint, Map<String, Object> properties) {
        this.apiEndpoint = apiEndpoint;
        this.properties = properties;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
