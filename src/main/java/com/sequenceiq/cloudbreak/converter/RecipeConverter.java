package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.KeyValueJson;
import com.sequenceiq.cloudbreak.controller.json.PluginJson;
import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.KeyValue;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class RecipeConverter extends AbstractConverter<RecipeJson, Recipe> {

    @Override
    public RecipeJson convert(Recipe recipe) {
        RecipeJson json = new RecipeJson();
        json.setName(recipe.getName());
        json.setDescription(recipe.getDescription());
        json.setPlugins(convertPlugins(recipe.getPlugins()));
        json.setId(recipe.getId().toString());
        json.setKeyValues(convertKeyValues(recipe.getKeyValues()));
        return json;
    }

    @Override
    public Recipe convert(RecipeJson json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setPlugins(convertPlugins(json.getPlugins(), recipe));
        recipe.setKeyValues(convertKeyValues(json.getKeyValues(), recipe));
        return recipe;
    }

    public Recipe convert(RecipeJson json, boolean publicInAccount) {
        Recipe recipe = convert(json);
        recipe.setPublicInAccount(publicInAccount);
        return recipe;
    }

    private List<Plugin> convertPlugins(List<PluginJson> pluginJsons, Recipe recipe) {
        List<Plugin> plugins = new ArrayList<>();
        for (PluginJson pluginJson : pluginJsons) {
            Plugin plugin = new Plugin();
            plugin.setUrl(pluginJson.getUrl());
            plugin.setName(getPluginName(pluginJson));
            plugin.setParameters(pluginJson.getParameters());
            plugin.setRecipe(recipe);
            plugins.add(plugin);
        }
        return plugins;
    }

    private List<KeyValue> convertKeyValues(List<KeyValueJson> kvJsons, Recipe recipe) {
        List<KeyValue> keyValues = new ArrayList<>();
        for (KeyValueJson kvJson : kvJsons) {
            KeyValue keyValue = new KeyValue();
            keyValue.setKey(kvJson.getKey());
            keyValue.setValue(kvJson.getValue());
            keyValue.setRecipe(recipe);
            keyValues.add(keyValue);
        }
        return keyValues;
    }

    private String getPluginName(PluginJson pluginJson) {
        String[] splits = pluginJson.getUrl().split("/");
        return splits[splits.length - 1].replace("consul-plugins-", "").replace(".git", "");
    }

    private List<PluginJson> convertPlugins(List<Plugin> plugins) {
        List<PluginJson> pluginJsons = new ArrayList<>();
        for (Plugin plugin : plugins) {
            PluginJson pluginJson = new PluginJson();
            pluginJson.setUrl(plugin.getUrl());
            pluginJson.setParameters(plugin.getParameters());
            pluginJsons.add(pluginJson);
        }
        return pluginJsons;
    }

    private List<KeyValueJson> convertKeyValues(List<KeyValue> keyValues) {
        List<KeyValueJson> kvJsons = new ArrayList<>();
        for (KeyValue keyValue : keyValues) {
            KeyValueJson kvJson = new KeyValueJson();
            kvJson.setKey(keyValue.getKey());
            kvJson.setValue(keyValue.getValue());
            kvJsons.add(kvJson);
        }
        return kvJsons;
    }
}
