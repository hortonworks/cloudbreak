package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;

public class RecipeToJsonConverterTest extends AbstractEntityConverterTest<Recipe> {

    private RecipeToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new RecipeToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        RecipeResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("id"));
    }

    @Override
    public Recipe createSource() {
        return TestUtil.recipes(1).iterator().next();
    }
}
