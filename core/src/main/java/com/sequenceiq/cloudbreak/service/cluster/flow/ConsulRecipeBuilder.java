package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class ConsulRecipeBuilder implements RecipeBuilder {

    @Override
    public List<Recipe> buildRecipes(String recipeName, List<RecipeScript> recipeScripts, Map<String, String> properties) {
        List<Recipe> recipes = new ArrayList<>();
        int index = 0;
        for (RecipeScript script : recipeScripts) {
            Recipe recipe = new Recipe();
            if (recipeScripts.size() > 1) {
                recipe.setName(recipeName + "-" + index);
            } else {
                recipe.setName(recipeName);
            }
            String tomlContent = new StringBuilder()
                    .append(String.format("[plugin]\nname=\"%s\"\ndescription=\"\"\nversion=\"1.0\"\n", recipeName))
                    .append("maintainer_name=\"Cloudbreak\"\n")
                    .append("[plugin.config]\n[plugin.compatibility]").toString();
            StringBuilder pluginContentBuilder = new StringBuilder();
            pluginContentBuilder.append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.getBytes())).append("\n");
            Set<Plugin> plugins = new HashSet<>();
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
            plugins.add(new Plugin("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().getBytes())));
            recipe.setPlugins(plugins);
            if (index == 0) {
                recipe.setKeyValues(properties);
            } else {
                recipe.setKeyValues(new HashMap<>());
            }
            index++;
            recipes.add(recipe);
        }

        return recipes;
    }
}
