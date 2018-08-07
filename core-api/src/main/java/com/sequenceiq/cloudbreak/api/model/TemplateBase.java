package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TemplateBase implements JsonEntity {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;

    @ApiModelProperty(TemplateModelDescription.PARAMETERS)
    private Map<String, Object> parameters = new HashMap<>();

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(TemplateModelDescription.VOLUME_TYPE)
    private String volumeType;

    @ApiModelProperty(TemplateModelDescription.ROOT_VOLUME_SIZE)
    private Integer rootVolumeSize;

    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    @ApiModelProperty(TemplateModelDescription.CUSTOM_INSTANCE_TYPE)
    private CustomInstanceType customInstanceType;

    @ApiModelProperty(ModelDescriptions.TOPOLOGY_ID)
    private Long topologyId;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String type) {
        cloudPlatform = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public CustomInstanceType getCustomInstanceType() {
        return customInstanceType;
    }

    public void setCustomInstanceType(CustomInstanceType customInstanceType) {
        this.customInstanceType = customInstanceType;
    }

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }
}
