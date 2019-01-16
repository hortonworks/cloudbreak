package com.sequenceiq.cloudbreak.converter.v4.recipes;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.domain.Recipe;

public class RecipeBaseToRecipeV4ConverterTest extends AbstractJsonConverterTest<RecipeV4Request> {

    private RecipeV4RequestToRecipeConverter underTest;

    @Before
    public void setUp() {
        underTest = new RecipeV4RequestToRecipeConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Recipe result = underTest.convert(getRequest("stack/recipe.json"));
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("plugins"));
    }

    @Override
    public Class<RecipeV4Request> getRequestClass() {
        return RecipeV4Request.class;
    }
}
