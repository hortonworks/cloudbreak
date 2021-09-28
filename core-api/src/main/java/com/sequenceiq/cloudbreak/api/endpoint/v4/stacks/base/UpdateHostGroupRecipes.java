package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateHostGroupRecipes implements JsonEntity {

    private String hostGroupName;

    private Set<String> recipeNames;

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }

    @Override
    public String toString() {
        return "UpdateHostGroupRecipes{" +
                "hostGroupName='" + hostGroupName + '\'' +
                ", recipeNames=" + recipeNames +
                '}';
    }
}
