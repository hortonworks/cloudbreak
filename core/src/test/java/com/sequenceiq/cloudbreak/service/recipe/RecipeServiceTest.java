package com.sequenceiq.cloudbreak.service.recipe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class RecipeServiceTest {

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
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(659602800L);
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString(), anyString());
        lenient().doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
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
    public void testCreateForLoggedInUser() {
        Recipe recipe = getRecipe();

        when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(mock(CloudbreakUser.class));
        when(userService.getOrCreate(any())).thenReturn(mock(User.class));
        Workspace workspace = mock(Workspace.class);
        when(workspaceService.get(eq(1L), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        when(recipeRepository.save(any())).thenReturn(recipe);

        String userCrn = CrnTestUtil.getUserCrnBuilder()
                .setResource("user_id")
                .setAccountId("account_id")
                .build().toString();
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.createForLoggedInUser(recipe, 1L, "account_id", userCrn));

        assertThat(recipe.getCreator(), is(userCrn));
        assertTrue(recipe.getResourceCrn().matches("crn:cdp:datahub:us-west-1:account_id:recipe:.*"));
    }

    @Test
    public void testCreateWithInternalUser() {
        Recipe recipe = getRecipe();
        recipe.setCreator(null);

        Workspace workspace = new Workspace();
        workspace.setId(1L);
        when(workspaceService.getByIdWithoutAuth(any())).thenReturn(workspace);
        when(recipeRepository.save(any())).thenReturn(recipe);

        Recipe savedRecipe = underTest.createWithInternalUser(recipe, 1L, "account_id");

        assertThat(recipe.getCreator(), nullValue());
        assertTrue(recipe.getResourceCrn().matches("crn:cdp:datahub:us-west-1:account_id:recipe:.*"));
        assertEquals(workspace, savedRecipe.getWorkspace());
        assertEquals(CreationType.SERVICE, savedRecipe.getCreationType());
    }

    private Recipe getRecipe() {
        Recipe recipe = new Recipe();
        recipe.setName("somename");
        recipe.setCreator("someone");
        recipe.setContent("bnllaGVoZSwgbmEgZXogZWd5IGZhc3phIGJhc2U2NCBjdWNj");
        recipe.setId(1L);
        recipe.setArchived(false);
        recipe.setResourceCrn(CrnTestUtil.getRecipeCrnBuilder()
                .setAccountId("account")
                .setResource("name")
                .build().toString());
        recipe.setWorkspace(getWorkspace());
        return recipe;
    }

    private Workspace getWorkspace() {
        Workspace ws = new Workspace();
        ws.setId(6L);
        return ws;
    }

}