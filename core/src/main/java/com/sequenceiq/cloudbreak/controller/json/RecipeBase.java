package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.RecipeModelDescription;
import com.sequenceiq.cloudbreak.controller.validation.TrustedPlugin;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Map;

public abstract class RecipeBase implements JsonEntity {
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

    @TrustedPlugin
    @ApiModelProperty(value = RecipeModelDescription.PLUGINS, required = true)
    private Map<String, PluginExecutionType> plugins;

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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, PluginExecutionType> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, PluginExecutionType> plugins) {
        this.plugins = plugins;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
