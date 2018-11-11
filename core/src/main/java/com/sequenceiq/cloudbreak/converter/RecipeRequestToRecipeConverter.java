package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class RecipeRequestToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Recipe convert(RecipeRequest source) {
        Recipe recipe = new Recipe();
        if (!Strings.isNullOrEmpty(source.getName())) {
            recipe.setName(source.getName());
        } else {
            String name = missingResourceNameGenerator.generateName(APIResourceType.RECIPE);

            if (source.getUri() != null) {
                String[] splitUrl = source.getUri().split("/");
                String lastPart = splitUrl[splitUrl.length - 1];
                String[] lastPartQueryArray = lastPart.split("\\?");
                name += lastPartQueryArray[0];

            }
            recipe.setName(name);
        }
        recipe.setDescription(source.getDescription());
        recipe.setRecipeType(source.getRecipeType());
        recipe.setContent(source.getContent());
        recipe.setUri(source.getUri());
        if (recipe.getUri() != null && recipe.getContent() == null) {
            recipe.setContent(fetchScriptContent(recipe.getUri()));
        }
        return recipe;
    }

    private String fetchScriptContent(String url) {
        try {
            String script = URLUtils.readUrl(url);
            return Base64.encodeBase64String(script.getBytes());
        } catch (IOException ignored) {
            throw new BadRequestException("Cannot download script from URL: " + url);
        }
    }
}
