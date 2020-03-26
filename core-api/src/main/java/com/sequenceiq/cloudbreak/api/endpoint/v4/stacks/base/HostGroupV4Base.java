package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class HostGroupV4Base implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(HostGroupModelDescription.RECIPE_IDS)
    private Set<String> recipeNames = new HashSet<>();

    @ApiModelProperty(value = HostGroupModelDescription.RECOVERY_MODE, allowableValues = "MANUAL,AUTO")
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    @ApiModelProperty(HostGroupModelDescription.INSTANCE_GROUP)
    private String instanceGroupName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRecipeNames() {
        return recipeNames;
    }

    public void setRecipeNames(Set<String> recipeNames) {
        this.recipeNames = recipeNames;
    }

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }
}
