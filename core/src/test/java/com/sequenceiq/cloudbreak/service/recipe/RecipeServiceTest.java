package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.dto.RecipeAccessDto.RecipeAccessDtoBuilder.aRecipeAccessDtoBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class RecipeServiceTest {

    private static final String INVALID_DTO_MESSAGE = "One and only one value of the crn and name should be filled!";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private RecipeService underTest;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeViewRepository recipeViewRepository;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private Clock clock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.getCurrentTimeMillis()).thenReturn(659602800L);
    }

    @Test
    public void testDeleteWhenDtoNameFilledThenDeleteCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.delete(aRecipeAccessDtoBuilder().withName(recipe.getName()).build(), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(1)).save(any(Recipe.class));
        verify(recipeRepository, times(1)).save(recipe);
    }

    @Test
    public void testDeleteWhenDtoCrnFilledThenDeleteCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.delete(aRecipeAccessDtoBuilder().withCrn(recipe.getResourceCrn()).build(), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(1)).save(any(Recipe.class));
        verify(recipeRepository, times(1)).save(recipe);
    }

    @Test
    public void testDeleteWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(INVALID_DTO_MESSAGE);

        underTest.delete(aRecipeAccessDtoBuilder().build(), 1L);

        verify(recipeRepository, times(0)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(0)).save(any());
    }

    @Test
    public void testDeleteIfDtoIsNullThenIllegalArgumentExceptionComes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("RecipeAccessDto should not be null");

        underTest.delete(null, 1L);
    }

    @Test
    public void testGetWhenDtoNameFilledThenProperGetCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.get(aRecipeAccessDtoBuilder().withName(recipe.getName()).build(), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId());
    }

    @Test
    public void testGetWhenDtoCrnFilledThenProperGetCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.get(aRecipeAccessDtoBuilder().withCrn(recipe.getResourceCrn()).build(), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId());
    }

    @Test
    public void testGetWhenNeitherCrnOrNameProvidedThenBadRequestExceptionComes() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(INVALID_DTO_MESSAGE);

        underTest.get(aRecipeAccessDtoBuilder().build(), 1L);

        verify(recipeRepository, times(0)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(0)).save(any());
    }

    @Test
    public void testGetIfDtoIsNullThenIllegalArgumentExceptionComes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("RecipeAccessDto should not be null");

        underTest.get(null, 1L);
    }

    private Recipe getRecipe() {
        Recipe recipe = new Recipe();
        recipe.setName("somename");
        recipe.setCreator("someone");
        recipe.setContent("bnllaGVoZSwgbmEgZXogZWd5IGZhc3phIGJhc2U2NCBjdWNj");
        recipe.setId(1L);
        recipe.setArchived(false);
        recipe.setResourceCrn("somecrn");
        recipe.setWorkspace(getWorkspace());
        return recipe;
    }

    private Workspace getWorkspace() {
        Workspace ws = new Workspace();
        ws.setId(6L);
        return ws;
    }

}