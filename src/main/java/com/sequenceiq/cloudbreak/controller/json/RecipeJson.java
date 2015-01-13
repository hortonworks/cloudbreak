package com.sequenceiq.cloudbreak.controller.json;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class RecipeJson implements JsonEntity {

    private String id;
    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    @Size(max = 1000)
    private String description;

    @Size(max = 1000)
    private String customerId;

    private List<PluginJson> plugins;

    @JsonProperty("keyvalues")
    private List<KeyValueJson> keyValues;

    private String blueprint;

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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @JsonRawValue
    public String getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(JsonNode node) {
        this.blueprint = node.toString();
    }

    public void setBlueprintFromText(String blueprintText) {
        this.blueprint = blueprintText;
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
