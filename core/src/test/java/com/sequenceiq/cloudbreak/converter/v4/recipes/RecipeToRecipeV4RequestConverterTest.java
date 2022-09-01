package com.sequenceiq.cloudbreak.converter.v4.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.Recipe;

@ExtendWith(MockitoExtension.class)
class RecipeToRecipeV4RequestConverterTest {

    @Spy
    private RecipeTypeToRecipeV4TypeConverter recipeTypeToRecipeV4TypeConverter;

    @InjectMocks
    private RecipeToRecipeV4RequestConverter recipeToRecipeV4RequestConverter;

    @Test
    public void testRecipeToRecipeV4RequestConverter() {
        Recipe source = new Recipe();
        source.setName("recipeName");
        source.setRecipeType(RecipeType.POST_SERVICE_DEPLOYMENT);
        source.setContent("content");
        source.setDescription("description");
        RecipeV4Request converted = recipeToRecipeV4RequestConverter.convert(source);
        assertEquals(converted.getName(), source.getName());
        assertEquals(converted.getType(), RecipeV4Type.POST_SERVICE_DEPLOYMENT);
        assertEquals(converted.getDescription(), source.getDescription());
        assertEquals(converted.getContent(), source.getContent());
    }

}