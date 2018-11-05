package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.VaultService;

@RunWith(MockitoJUnitRunner.class)
public class RecipeToRecipeResponseConverterTest extends AbstractEntityConverterTest<Recipe> {

    @Mock
    private ConversionService conversionService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private RecipeToRecipeResponseConverter underTest;

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Recipe recipe = getSource();
        when(conversionService.convert(recipe.getWorkspace(), WorkspaceResourceResponse.class)).thenReturn(new WorkspaceResourceResponse());
        when(vaultService.resolveSingleValue(any())).thenReturn(recipe.getContent());
        RecipeResponse result = underTest.convert(recipe);
        // THEN
        assertAllFieldsNotNull(result, Lists.newArrayList("id", "plugins"));
    }

    @Override
    public Recipe createSource() {
        return TestUtil.recipes(1).iterator().next();
    }
}
