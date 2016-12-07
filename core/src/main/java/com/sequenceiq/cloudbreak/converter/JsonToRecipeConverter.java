package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {
    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        if (json.getName() != null) {
            recipe.setName(json.getName());
        } else {
            String name = "hrec" + System.nanoTime();

            if (json.getUri() != null) {
                String[] splitUrl = json.getUri().split("/");
                String lastPart = splitUrl[splitUrl.length - 1];
                String[] lastPartQueryArray = lastPart.split("\\?");
                name += lastPartQueryArray[0];

            }
            recipe.setName(name);

        }
        recipe.setDescription(json.getDescription());
        recipe.setRecipeType(json.getRecipeType());
        recipe.setContent(json.getContent());
        recipe.setUri(json.getUri());
        if (recipe.getUri() != null && recipe.getContent() == null) {
            recipe.setContent(fetchScriptContent(recipe.getUri()));
        }
        return recipe;
    }

    private String fetchScriptContent(String url) {
        try {
            String script = URLUtils.readUrl(url);
            return Base64.encodeBase64String(script.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Cannot download script from URL: " + url);
        }
    }

}
