package com.sequenceiq.cloudbreak.converter.v4.recipes;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;

@RunWith(MockitoJUnitRunner.class)
public class RecipeToRecipeV4ResponseConverterTest extends AbstractEntityConverterTest<Recipe> {

    @Mock
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @InjectMocks
    private RecipeToRecipeV4ResponseConverter underTest;

    @Test
    public void testConvert() {
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
