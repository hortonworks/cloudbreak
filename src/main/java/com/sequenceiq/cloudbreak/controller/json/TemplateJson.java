package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.validation.ValidProvisionRequest;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@ValidProvisionRequest
public class TemplateJson implements JsonEntity {

    private Long id;
    private CloudPlatform cloudPlatform;
    @NotNull
    @Size(max = 20, min = 5)
    private String name;
    private Map<String, Object> parameters = new HashMap<>();
    @Size(max = 50)
    private String description;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform type) {
        this.cloudPlatform = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

}
