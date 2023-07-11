package com.sequenceiq.sdx.api.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterRequest extends SdxClusterRequestBase {

    @ApiModelProperty(ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    @ApiModelProperty(ModelDescriptions.OS)
    private String os;

    @ApiModelProperty(ModelDescriptions.RECIPES)
    private Set<SdxRecipe> recipes;

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Set<SdxRecipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(Set<SdxRecipe> recipes) {
        this.recipes = recipes;
    }

    @Override
    public String toString() {
        return "SdxClusterRequest{" +
                "runtime='" + runtime + '\'' +
                ", os='" + os + '\'' +
                ", recipes=" + recipes +
                "} " + super.toString();
    }
}
