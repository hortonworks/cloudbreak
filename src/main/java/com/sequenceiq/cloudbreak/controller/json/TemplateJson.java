package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.validation.ValidProvisionRequest;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@ValidProvisionRequest
public class TemplateJson implements JsonEntity {

    private Long id;
    private CloudPlatform cloudPlatform;
    @Size(max = 100, min = 5, message = "The length of the template's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name of the template can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    private Map<String, Object> parameters = new HashMap<>();
    @Size(max = 1000)
    private String description;
    @Min(value = 1, message = "The number of volumes has to be greater than 0")
    @Max(value = 15, message = "The number of volumes has to be less than 15")
    @NotNull
    private Integer volumeCount;
    @Min(value = 10, message = "The size of the volume has to be greater than or equal to 10 GB")
    @Max(value = 1000, message = "The size of the volume has to be less than or equal to 1000 GB")
    @NotNull
    private Integer volumeSize;
    private boolean publicInAccount;

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
