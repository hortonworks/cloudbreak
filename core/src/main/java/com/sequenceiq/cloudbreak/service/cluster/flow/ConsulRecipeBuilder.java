package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class ConsulRecipeBuilder implements RecipeBuilder {

    private static final String RECIPE_KEY_PREFIX = "recipes/built-in/";
    private static final int NAME_LENGTH = 10;

    @Override
    public List<Recipe> buildRecipes(List<RecipeScript> recipeScripts, Map<String, String> properties) {
        List<Recipe> recipes = new ArrayList<>();
        int index = 0;
        for (RecipeScript script : recipeScripts) {
            Recipe recipe = new Recipe();
            String recipeName = RandomStringUtils.random(NAME_LENGTH, true, true);
            recipe.setName(recipeName);
            String tomlContent = new StringBuilder()
                    .append(String.format("[plugin]\nname=\"%s\"\ndescription=\"\"\nversion=\"1.0\"\n", recipeName))
                    .append("maintainer_name=\"Cloudbreak\"\n")
                    .append("[plugin.config]\n[plugin.compatibility]").toString();
            StringBuilder pluginContentBuilder = new StringBuilder();
            pluginContentBuilder.append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.getBytes())).append("\n");
            Map<String, PluginExecutionType> plugins = new HashMap<>();
            switch (script.getClusterLifecycleEvent()) {
                case PRE_INSTALL:
                    pluginContentBuilder.append("recipe-pre-install:").append(Base64.encodeBase64String(script.getScript().getBytes())).append("\n");
                    break;
                case POST_INSTALL:
                    pluginContentBuilder.append("recipe-post-install:").append(Base64.encodeBase64String(script.getScript().getBytes())).append("\n");
                    break;
                default:
                    throw new UnsupportedOperationException("Cluster lifecycle event " + script.getClusterLifecycleEvent() + " is not supported");
            }
            plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().getBytes()), script.getExecutionType());
            recipe.setPlugins(plugins);
            recipe.setTimeout(DEFAULT_RECIPE_TIMEOUT);
            if (index == 0) {
                recipe.setKeyValues(properties);
            } else {
                recipe.setKeyValues(new HashMap<String, String>());
            }
            index++;
            recipes.add(recipe);
        }

        return recipes;
    }
}
