package com.sequenceiq.cloudbreak.converter;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;
import org.junit.Before;
import org.junit.Test;

public class RecipeToRecipeResponseConverterTest extends AbstractEntityConverterTest<Recipe> {

    private RecipeToRecipeResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new RecipeToRecipeResponseConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        RecipeResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Lists.newArrayList("id", "plugins"));
    }

    @Override
    public Recipe createSource() {
        return TestUtil.recipes(1).iterator().next();
    }
}
