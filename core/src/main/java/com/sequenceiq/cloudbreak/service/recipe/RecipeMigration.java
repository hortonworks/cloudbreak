package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.RecipeType;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;

@Service
public class RecipeMigration {

    private static final String PRE_INSTALL_TAG = "recipe-pre-install";

    private static final String POST_INSTALL_TAG = "recipe-post-install";

    @Inject
    private RecipeRepository recipeRepository;

    @Transactional
    public void migrate() {
        Set<Recipe> recipes = recipeRepository.findByType(RecipeType.LEGACY);
        for (Recipe legacyRecipe : recipes) {
            for (Plugin plugin : legacyRecipe.getPlugins()) {
                String decodedRecipe = new String(Base64.decodeBase64(plugin.getContent().replaceFirst("base64://", "")));
                Map<String, String> recipeMap = Stream.of(decodedRecipe.split("\n"))
                        .collect(Collectors.toMap(s -> s.substring(0, s.indexOf(":")), s -> s.substring(s.indexOf(":") + 1)));

                if (recipeMap.containsKey(PRE_INSTALL_TAG)) {
                    create(legacyRecipe, RecipeType.PRE, recipeMap.get(PRE_INSTALL_TAG));
                }
                if (recipeMap.containsKey(POST_INSTALL_TAG)) {
                    create(legacyRecipe, RecipeType.POST, recipeMap.get(POST_INSTALL_TAG));
                }
            }
            legacyRecipe.setRecipeType(RecipeType.MIGRATED);
            recipeRepository.save(legacyRecipe);
        }
    }

    private void create(Recipe legacyRecipe, RecipeType recipeType, String content) {
        Recipe recipe = new Recipe();
        recipe.setName(recipeType.name().toLowerCase() + "-" + legacyRecipe.getName());
        recipe.setOwner(legacyRecipe.getOwner());
        recipe.setAccount(legacyRecipe.getAccount());
        recipe.setDescription(legacyRecipe.getDescription());
        recipe.setRecipeType(recipeType);
        recipe.setContent(content);
        recipeRepository.save(recipe);

    }

}
