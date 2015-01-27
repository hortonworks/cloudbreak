package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;



public class RecipeJson implements JsonEntity {

    private String id;
    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    @Size(max = 1000)
    private String description;

    @Valid
    private List<PluginJson> plugins;

    @JsonProperty("keyvalues")
    private List<KeyValueJson> keyValues;

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

    public List<PluginJson> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginJson> plugins) {
        this.plugins = plugins;
    }

    public List<KeyValueJson> getKeyValues() {
        return keyValues;
    }

    public void setKeyValues(List<KeyValueJson> keyValues) {
        this.keyValues = keyValues;
    }
}
