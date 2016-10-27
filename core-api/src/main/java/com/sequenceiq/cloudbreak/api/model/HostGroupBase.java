package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class HostGroupBase {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @Valid
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.CONSTRAINT, required = true)
    private ConstraintJson constraint;

    @ApiModelProperty(value = HostGroupModelDescription.RECIPE_IDS)
    private Set<Long> recipeIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConstraintJson getConstraint() {
        return constraint;
    }

    public void setConstraint(ConstraintJson constraint) {
        this.constraint = constraint;
    }

    public Set<Long> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(Set<Long> recipeIds) {
        this.recipeIds = recipeIds;
    }

}
