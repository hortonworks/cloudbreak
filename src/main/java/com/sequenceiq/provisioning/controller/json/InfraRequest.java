package com.sequenceiq.provisioning.controller.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.provisioning.controller.validation.ValidProvisionRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.json.JsonEntity;

@ValidProvisionRequest
public class InfraRequest implements JsonEntity {

    private Long id;
    private CloudPlatform cloudPlatform;
    private String clusterName;
    private Map<String, String> parameters;

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform type) {
        this.cloudPlatform = type;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
