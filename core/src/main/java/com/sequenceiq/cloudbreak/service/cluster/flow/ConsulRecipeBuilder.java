package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent.PRE_INSTALL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.util.Base64;

import com.sequenceiq.cloudbreak.domain.Recipe;

public class ConsulRecipeBuilder implements RecipeBuilder {

    @Override
    public List<Recipe> buildRecipes(List<FileSystemScript> fileSystemScripts, Map<String, String> properties) {
        List<Recipe> recipes = new ArrayList<>();
        for (FileSystemScript script : fileSystemScripts) {
            Recipe recipe = new Recipe();
            String url = "consul://" + RECIPE_KEY_PREFIX + recipeStored.getName();
            String tomlContent = new StringBuilder()
                    .append(String.format("[plugin]\nname=\"%s\"\ndescription=\"%s\"\nversion=\"1.0\"\n", recipeStored.getName(), recipeStored.getDescription()))
                    .append(String.format("maintainer_name=Cloudbreak\"website_url=\"%s\"\n", url))
                    .append("[plugin.config]\n[plugin.compatibility]").toString();
            StringBuilder pluginContentBuilder = new StringBuilder();
            pluginContentBuilder.append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.getBytes())).append("\n");
            switch (script.getClusterLifecycleEvent()){
                case PRE_INSTALL:
                    pluginContentBuilder.append("recipe-pre-install:").append(script.getScript()).append("\n");
                    break;
                case POST_INSTALL:
                    pluginContentBuilder.append("recipe-post-install:").append(script.getScript()).append("\n");
                    break;
                default:
                    throw new UnsupportedOperationException("Cluster lifecycle event " + script.getClusterLifecycleEvent() + " is not supported");
            }
            recipe.setPluginContent(pluginContentBuilder.toString());
            recipes.add(recipe);
        }

        //recipe.setKeyValues(properties);
//        recipe.set

        return recipes;
    }
}
