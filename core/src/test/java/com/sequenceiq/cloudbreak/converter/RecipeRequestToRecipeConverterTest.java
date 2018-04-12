package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.domain.Recipe;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

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
