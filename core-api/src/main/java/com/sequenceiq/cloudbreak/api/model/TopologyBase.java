package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TopologyBase implements JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the topology's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The topology's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Size(max = 1000, message = "The length of the topology's description has to be less than 1000")
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.TopologyModelDescription.NODES)
    private Map<String, String> nodes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Map<String, String> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, String> nodes) {
        this.nodes = nodes;
    }
}
