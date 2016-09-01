package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidPlugin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
abstract class RecipeBase implements JsonEntity {
    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @JsonPropertyDescription("Recipe timeout in minutes.")
    @ApiModelProperty(RecipeModelDescription.TIMEOUT)
    private Integer timeout;

    @ValidPlugin
    @ApiModelProperty(value = RecipeModelDescription.PLUGINS)
    private Set<String> plugins;

    @JsonProperty("properties")
    @ApiModelProperty(value = RecipeModelDescription.PROPERTIES)
    private Map<String, String> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Set<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(Set<String> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
