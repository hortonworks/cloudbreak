package com.sequenceiq.cloudbreak.service.recipe;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class RecipeServiceTest {

    private static final String INVALID_DTO_MESSAGE = "One and only one value of the crn and name should be filled!";

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

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.getCurrentTimeMillis()).thenReturn(659602800L);
    }

    @Test
    public void testDeleteWhenDtoNameFilledThenDeleteCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.delete(NameOrCrn.ofName(recipe.getName()), recipe.getWorkspace().getId());

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

        Recipe result = underTest.delete(NameOrCrn.ofCrn(recipe.getResourceCrn()), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(1)).save(any(Recipe.class));
        verify(recipeRepository, times(1)).save(recipe);
    }

    @Test
    public void testGetWhenDtoNameFilledThenProperGetCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.get(NameOrCrn.ofName(recipe.getName()), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId());
    }

    @Test
    public void testGetWhenDtoCrnFilledThenProperGetCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));

        Recipe result = underTest.get(NameOrCrn.ofCrn(recipe.getResourceCrn()), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId());
    }

    @Test
    public void testPopulateCrnCorrectly() {
        Recipe recipe = getRecipe();

        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(mock(CloudbreakUser.class));
        when(userService.getOrCreate(any())).thenReturn(mock(User.class));
        Workspace workspace = mock(Workspace.class);
        when(workspaceService.get(eq(1L), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));

        underTest.createForLoggedInUser(recipe, 1L, "account_id", "creator");

        assertThat(recipe.getCreator(), is("creator"));
        assertTrue(recipe.getResourceCrn().matches("crn:cdp:datahub:us-west-1:account_id:recipe:.*"));
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