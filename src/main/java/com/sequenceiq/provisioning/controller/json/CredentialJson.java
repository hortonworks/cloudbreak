package com.sequenceiq.provisioning.controller.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.provisioning.controller.validation.ValidCredentialRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@ValidCredentialRequest
public class CredentialJson implements JsonEntity {

    private Long id;
    private String name;
    private CloudPlatform cloudPlatform;
    private Map<String, String> parameters;

    public CredentialJson() {

    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
