package com.sequenceiq.cloudbreak.converter.v4.recipes;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;

@ExtendWith(MockitoExtension.class)
class RecipeToRecipeV4ResponseConverterTest extends AbstractEntityConverterTest<Recipe> {

    @Spy
    private RecipeTypeToRecipeV4TypeConverter recipeTypeToRecipeV4TypeConverter;

    @Mock
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @InjectMocks
    private RecipeToRecipeV4ResponseConverter underTest;

    @Test
    void testConvert() {
        // GIVEN
        // WHEN
        Recipe recipe = getSource();
        when(workspaceToWorkspaceResourceV4ResponseConverter.convert(recipe.getWorkspace()))
                .thenReturn(new WorkspaceResourceV4Response());
        RecipeV4Response result = underTest.convert(recipe);
        // THEN
        assertAllFieldsNotNull(result, List.of("created"));
    }

    @Override
    public Recipe createSource() {
        return TestUtil.recipes(1).iterator().next();
    }
}
