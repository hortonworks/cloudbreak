package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
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
            recipe.setName(missingResourceNameGenerator.generateName(APIResourceType.RECIPE));
        }
        recipe.setDescription(source.getDescription());
        recipe.setRecipeType(source.getRecipeType());
        recipe.setContent(source.getContent());
        return recipe;
    }
}
