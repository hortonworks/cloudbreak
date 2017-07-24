package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        if (!Strings.isNullOrEmpty(json.getName())) {
            recipe.setName(json.getName());
        } else {
            String name = missingResourceNameGenerator.generateName(APIResourceType.RECIPE);

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
