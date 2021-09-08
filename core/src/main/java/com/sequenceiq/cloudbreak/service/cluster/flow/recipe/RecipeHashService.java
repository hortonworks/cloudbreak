package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@Component
public class RecipeHashService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeHashService.class);

    public Map<String, String> generateRecipeHashes(HostGroup hostGroup) {
        Map<String, String> map = new HashMap<>();
        Set<Recipe> recipes = hostGroup.getRecipes();
        for (Recipe recipe : recipes) {
            map.put(recipe.getName(), DigestUtils.sha256Hex(recipe.getContent()));
        }
        return map;
    }

    public boolean checkRecipeHashes(Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            Set<Recipe> recipes = hostGroup.getRecipes();
            LOGGER.debug("Host group '{}' does not have any recipes", hostGroup.getName());
            Map<String, String> recipeHashes = hostGroup.getRecipeHashes();
            if (recipeHashes == null && CollectionUtils.isNotEmpty(recipes)) {
                LOGGER.debug("Host group '{}' does not have any recipe hashes, it will be set first time.", hostGroup.getName());
                return false;
            }
            // TODO: if no any recipes
            for (Recipe recipe : recipes) {
                String recipeName = recipe.getName();
                String recipeContent = recipe.getContent();
                if (recipeHashes != null && recipeHashes.containsKey(recipeName)) {
                    String sha256HexForRecipe = DigestUtils.sha256Hex(recipeContent);
                    String sha256InHostGroup = recipeHashes.get(recipeName);
                    if (!sha256HexForRecipe.equals(sha256InHostGroup)) {
                        LOGGER.debug("Recipe hash for {} recipe does not match for current recipe content [host group: {}].", recipeName, hostGroup.getName());
                        return false;
                    }
                } else {
                    LOGGER.debug("No recipe hash for '{}'", recipeName);
                    return false;
                }
            }
        }
        LOGGER.debug("As no change in recipe hashes, no need for updating recipes.");
        return true;
    }
}
