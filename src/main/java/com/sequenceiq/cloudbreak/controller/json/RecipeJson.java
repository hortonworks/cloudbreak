package com.sequenceiq.cloudbreak.controller.json;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sequenceiq.cloudbreak.controller.validation.TrustedPlugin;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;

@ApiModel("Recipe")
public class RecipeJson implements JsonEntity {

    private String id;
    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(required = true)
    private String name;
    @Size(max = 1000)
    private String description;
    @JsonPropertyDescription("Recipe timeout in minutes.")
    private Integer timeout;

    @TrustedPlugin
    @ApiModelProperty(required = true)
    private Map<String, PluginExecutionType> plugins;

    @JsonProperty("properties")
    private Map<String, String> properties;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setId(String id) {
        this.id = id;
    }

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
