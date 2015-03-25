package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TemplateBase implements JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private CloudPlatform cloudPlatform;
    @Size(max = 100, min = 5, message = "The length of the template's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name of the template can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.PARAMETERS, required = true)
    private Map<String, Object> parameters = new HashMap<>();
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.VOLUME_COUNT, required = true)
    private Integer volumeCount;
    @ApiModelProperty(ModelDescriptions.TemplateModelDescription.VOLUME_SIZE)
    private Integer volumeSize;
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, readOnly = true)
    private boolean publicInAccount;

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

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
