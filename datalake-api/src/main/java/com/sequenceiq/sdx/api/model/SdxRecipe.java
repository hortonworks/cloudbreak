package com.sequenceiq.sdx.api.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRecipe implements Serializable {

    @NotBlank
    @Schema(description = ModelDescriptions.RECIPE_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Schema(description = ModelDescriptions.HOST_GROUP_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String hostGroup;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    @Override
    public String toString() {
        return "SdxRecipe{" + "name='" + name + '\'' + ", hostGroup='" + hostGroup + '\'' + '}';
    }
}
