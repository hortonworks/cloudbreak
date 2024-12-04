package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RefreshClusterRecipeV4Base implements JsonEntity {

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipeName;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String hostGroupName;

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    @Override
    public String toString() {
        return "RefreshClusterRecipeV4Base{" +
                "recipeName='" + recipeName + '\'' +
                ", hostGroupName='" + hostGroupName + '\'' +
                '}';
    }
}
