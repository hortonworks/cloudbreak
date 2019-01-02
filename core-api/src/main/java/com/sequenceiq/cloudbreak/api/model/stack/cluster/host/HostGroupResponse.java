package com.sequenceiq.cloudbreak.api.model.stack.cluster.host;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupResponse extends HostGroupBase {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(HostGroupModelDescription.RECIPES)
    private Set<RecipeV4Response> recipes;

    @ApiModelProperty(HostGroupModelDescription.EXTENDED_RECIPES)
    private Set<String> extendedRecipes;

    @ApiModelProperty(HostGroupModelDescription.METADATA)
    private Set<HostMetadataResponse> metadata = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<HostMetadataResponse> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<HostMetadataResponse> metadata) {
        this.metadata = metadata;
    }

    public Set<RecipeV4Response> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<RecipeV4Response> recipes) {
        this.recipes = recipes;
    }

    public Set<String> getExtendedRecipes() {
        return extendedRecipes;
    }

    public void setExtendedRecipes(Set<String> extendedRecipes) {
        this.extendedRecipes = extendedRecipes;
    }
}
