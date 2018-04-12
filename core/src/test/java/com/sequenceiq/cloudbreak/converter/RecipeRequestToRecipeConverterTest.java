package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.domain.Recipe;

public class RecipeRequestToRecipeConverterTest extends AbstractJsonConverterTest<RecipeRequest> {

    private RecipeRequestToRecipeConverter underTest;

    @Before
    public void setUp() {
        underTest = new RecipeRequestToRecipeConverter();
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
    public Class<RecipeRequest> getRequestClass() {
        return RecipeRequest.class;
    }
}
