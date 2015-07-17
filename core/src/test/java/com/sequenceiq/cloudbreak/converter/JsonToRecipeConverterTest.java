package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.RecipeRequest;
import com.sequenceiq.cloudbreak.domain.Recipe;

public class JsonToRecipeConverterTest extends AbstractJsonConverterTest<RecipeRequest> {

    private JsonToRecipeConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToRecipeConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Recipe result = underTest.convert(getRequest("stack/recipe.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<RecipeRequest> getRequestClass() {
        return RecipeRequest.class;
    }
}
