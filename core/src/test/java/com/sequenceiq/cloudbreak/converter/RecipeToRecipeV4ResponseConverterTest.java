package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Mockito.when;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4ResponseConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.domain.Recipe;

@RunWith(MockitoJUnitRunner.class)
public class RecipeToRecipeV4ResponseConverterTest extends AbstractEntityConverterTest<Recipe> {

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private RecipeToRecipeV4ResponseConverter underTest;

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Recipe recipe = getSource();
        when(conversionService.convert(recipe.getWorkspace(), WorkspaceResourceV4Response.class)).thenReturn(new WorkspaceResourceV4Response());
        RecipeV4Response result = underTest.convert(recipe);
        // THEN
        assertAllFieldsNotNull(result, Lists.newArrayList("id", "plugins"));
    }

    @Override
    public Recipe createSource() {
        return TestUtil.recipes(1).iterator().next();
    }
}
