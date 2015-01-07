package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.PluginJson;
import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.Blueprint;
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
        json.setCustomerId(recipe.getCustomerId());
        json.setId(recipe.getId().toString());
        json.setBlueprintFromText(recipe.getBlueprint().getBlueprintText());
        return json;
    }

    @Override
    public Recipe convert(RecipeJson json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setCustomerId(json.getCustomerId());
        recipe.setDescription(json.getDescription());
        recipe.setPlugins(convertPlugins(json.getPlugins(), recipe));
        return recipe;
    }

    public Recipe convert(RecipeJson json, Blueprint blueprint, boolean publicInAccount) {
        Recipe recipe = convert(json);
        recipe.setBlueprint(blueprint);
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
}
