package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("HostGroup")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupJson {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @NotNull
    @ApiModelProperty(value = HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    private String instanceGroupName;
    @ApiModelProperty(value = HostGroupModelDescription.RECIPE_IDS)
    private Set<Long> recipeIds;

    private Set<HostMetadataJson> metadata = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }

    public Set<Long> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(Set<Long> recipeIds) {
        this.recipeIds = recipeIds;
    }

    public Set<HostMetadataJson> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<HostMetadataJson> metadata) {
        this.metadata = metadata;
    }
}
