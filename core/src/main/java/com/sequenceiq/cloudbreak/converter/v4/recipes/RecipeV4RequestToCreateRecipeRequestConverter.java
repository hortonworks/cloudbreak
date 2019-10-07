package com.sequenceiq.cloudbreak.converter.v4.recipes;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.datahub.model.CreateRecipeRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class RecipeV4RequestToCreateRecipeRequestConverter extends AbstractConversionServiceAwareConverter<RecipeV4Request, CreateRecipeRequest> {

    @Override
    public CreateRecipeRequest convert(RecipeV4Request source) {
        CreateRecipeRequest recipe = new CreateRecipeRequest();
        recipe.setRecipeName(source.getName());
        recipe.setDescription(source.getDescription());
        recipe.setType(source.getType().name());
        recipe.setRecipeContent(source.getContent());
        return recipe;
    }
}
