package com.sequenceiq.sdx.api.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRecipe implements Serializable {

    @ApiModelProperty(ModelDescriptions.RECIPE_NAME)
    @NotBlank
    private String name;

    @ApiModelProperty(ModelDescriptions.HOST_GROUP_NAME)
    @NotBlank
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
        return "SdxRecipe{" +
                "name='" + name + '\'' +
                ", hostGroup='" + hostGroup + '\'' +
                '}';
    }
}
