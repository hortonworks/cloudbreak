package com.sequenceiq.cloudbreak.service.recipe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.usage.service.RecipeUsageService;
import com.sequenceiq.cloudbreak.util.TestConstants;
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
    private ClusterService clusterService;

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

    @Mock
    private FreeipaClientService freeipaClientService;

    @Mock
    private RecipeUsageService recipeUsageService;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
        lenient().when(clock.getCurrentTimeMillis()).thenReturn(659602800L);
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString());
        lenient().doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        lenient().doNothing().when(recipeUsageService).sendDeletedUsageReport(anyString(), anyString(), anyString());
        lenient().doNothing().when(recipeUsageService).sendCreatedUsageReport(anyString(), anyString(), anyString());
    }

    @Test
    public void testClusterCreationUsageSending() {
        Stack stack = stack(Set.of(
                hostGroup("hostGroup1", Set.of(
                        recipeWithType("rp1", RecipeType.PRE_SERVICE_DEPLOYMENT),
                        recipeWithType("rp2", RecipeType.POST_SERVICE_DEPLOYMENT),
                        recipeWithType("rp3", RecipeType.PRE_TERMINATION)
                )),
                hostGroup("hostGroup2", Set.of(
                        recipeWithType("rp4", RecipeType.POST_CLOUDERA_MANAGER_START),
                        recipeWithType("rp2", RecipeType.POST_SERVICE_DEPLOYMENT),
                        recipeWithType("rp5", RecipeType.POST_CLOUDERA_MANAGER_START),
                        recipeWithType("rp6", RecipeType.POST_CLUSTER_INSTALL)
                ))
        ));
        doNothing().when(recipeUsageService).sendClusterCreationRecipeUsageReport(anyString(), anyInt(), any(), any());
        underTest.sendClusterCreationUsageReport(stack);
        ArgumentCaptor<Optional<String>> typeDetailsCaptor = ArgumentCaptor.forClass(Optional.class);
        ArgumentCaptor<Optional<String>> hostGroupDetailsCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(recipeUsageService).sendClusterCreationRecipeUsageReport(eq("crn"), eq(7),
                typeDetailsCaptor.capture(), hostGroupDetailsCaptor.capture());
        assertTrue(typeDetailsCaptor.getValue().isPresent());
        assertTrue(hostGroupDetailsCaptor.getValue().isPresent());
        String typeDetailsCaptorValue = typeDetailsCaptor.getValue().get();
        String hostGroupDetailsCaptorValue = hostGroupDetailsCaptor.getValue().get();
        assertTrue(JsonUtil.isValid(typeDetailsCaptorValue));
        assertTrue(JsonUtil.isValid(hostGroupDetailsCaptorValue));
        Map<RecipeType, Integer> typeDetails = JsonUtil.jsonToType(typeDetailsCaptorValue, new TypeReference<>() {
        });
        Map<String, Integer> hostGroupDetails = JsonUtil.jsonToType(hostGroupDetailsCaptorValue, new TypeReference<>() {
        });
        assertEquals(RecipeType.values().length, typeDetails.entrySet().size());
        assertEquals(2, typeDetails.get(RecipeType.POST_CLOUDERA_MANAGER_START));
        assertEquals(3, hostGroupDetails.get("hostGroup1"));
        assertEquals(4, hostGroupDetails.get("hostGroup2"));
    }

    @Test
    public void testDeleteButRecipeUsedInFMS() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));
        when(freeipaClientService.recipes(any())).thenReturn(List.of(recipe.getName()));

        assertThrows(BadRequestException.class, () -> underTest.delete(NameOrCrn.ofName(recipe.getName()), recipe.getWorkspace().getId()));

        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(0)).delete(any());
    }

    @Test
    public void testMultipleDeleteBut1RecipeUsedInFMS() {
        Recipe recipe1 = getRecipe(1L);
        Recipe recipe2 = getRecipe(2L);
        when(freeipaClientService.recipes(any())).thenReturn(List.of(recipe1.getName()));
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> {
            assertThrows(BadRequestException.class, () -> underTest.delete(Set.of(recipe1, recipe2)));
        });

        verify(recipeRepository, atMostOnce()).delete(any());
    }

    @Test
    public void testMultipleDeleteButBothRecipeUsedInFMS() {
        Recipe recipe1 = getRecipe(1L);
        Recipe recipe2 = getRecipe(2L);
        when(freeipaClientService.recipes(any())).thenReturn(List.of(recipe1.getName(), recipe2.getName()));
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> {
            assertThrows(BadRequestException.class, () -> underTest.delete(Set.of(recipe1, recipe2)));
        });

        verify(recipeRepository, times(0)).delete(any());
    }

    @Test
    public void testMultipleByNameButRecipeUsedInFMS() {
        Recipe recipe1 = getRecipe(1L);
        Recipe recipe2 = getRecipe(2L);
        when(recipeRepository.findByNameInAndWorkspaceId(Set.of(recipe1.getName(), recipe2.getName()), recipe1.getWorkspace().getId()))
                .thenReturn(Set.of(recipe1, recipe2));
        when(freeipaClientService.recipes(any())).thenReturn(List.of(recipe1.getName()));
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> {
            assertThrows(BadRequestException.class, () -> underTest.deleteMultipleByNameFromWorkspace(Set.of(recipe1.getName(), recipe2.getName()),
                    recipe1.getWorkspace().getId()));
        });

        verify(recipeRepository, atMostOnce()).delete(any());
    }

    @Test
    public void testDeleteWhenDtoNameFilledThenDeleteCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));
        when(freeipaClientService.recipes(any())).thenReturn(Collections.emptyList());

        Recipe result = underTest.delete(NameOrCrn.ofName(recipe.getName()), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByNameAndWorkspaceId(recipe.getName(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(1)).delete(any(Recipe.class));
        verify(recipeRepository, times(1)).delete(recipe);
    }

    @Test
    public void testDeleteWhenDtoCrnFilledThenDeleteCalled() {
        Recipe recipe = getRecipe();
        when(recipeRepository.findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId())).thenReturn(Optional.of(recipe));
        when(freeipaClientService.recipes(any())).thenReturn(Collections.emptyList());

        Recipe result = underTest.delete(NameOrCrn.ofCrn(recipe.getResourceCrn()), recipe.getWorkspace().getId());

        assertEquals(recipe, result);
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(anyString(), anyLong());
        verify(recipeRepository, times(1)).findByResourceCrnAndWorkspaceId(recipe.getResourceCrn(), recipe.getWorkspace().getId());
        verify(recipeRepository, times(1)).delete(any(Recipe.class));
        verify(recipeRepository, times(1)).delete(recipe);
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
        assertTrue(recipe.getResourceCrn().matches("crn:cdp:recipe:us-west-1:account_id:recipe:.*"));
    }

    @Test
    public void testCreateWhenAccountReachedMaxRecipeLimit() {
        Recipe recipe = getRecipe();
        when(recipeRepository.countByAccountId("account_id")).thenReturn(5001);

        String userCrn = CrnTestUtil.getUserCrnBuilder()
                .setResource("user_id")
                .setAccountId("account_id")
                .build().toString();
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.createForLoggedInUser(recipe, 1L, "account_id", userCrn)));

        assertEquals("Max recipe limit reached in account. Please remove unused recipes.", exception.getMessage());
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
        assertTrue(recipe.getResourceCrn().matches("crn:cdp:recipe:us-west-1:account_id:recipe:.*"));
        assertEquals(workspace, savedRecipe.getWorkspace());
        assertEquals(CreationType.SERVICE, savedRecipe.getCreationType());
    }

    @Test
    public void testPrepareDeletionWithNullRecipe() {
        NotFoundException exc = assertThrows(NotFoundException.class, () -> underTest.prepareDeletion(null));
        assertEquals("Recipe not found.", exc.getMessage());
    }

    @Test
    public void testPrepareDeletionWithSingleCluster() {
        Recipe recipe = getRecipe();
        when(clusterService.findAllClusterNamesByRecipeId(eq(recipe.getId()))).thenReturn(Set.of("cluster1"));
        BadRequestException exc = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(recipe));
        assertEquals("There is a cluster ['cluster1'] which uses recipe 'somename1'." +
                " Please remove this cluster before deleting the recipe.", exc.getMessage());
    }

    @Test
    public void testPrepareDeletionWithMultipleCluster() {
        Recipe recipe = getRecipe();
        when(clusterService.findAllClusterNamesByRecipeId(eq(recipe.getId()))).thenReturn(Set.of("cluster1", "cluster2"));
        BadRequestException exc = assertThrows(BadRequestException.class, () -> underTest.prepareDeletion(recipe));
        assertEquals("There are clusters associated with recipe 'somename1'. Please remove these before deleting the recipe." +
                " The following clusters are using this recipe: [cluster1, cluster2].", exc.getMessage());
    }

    private Recipe getRecipe() {
        return getRecipe(1L);
    }

    private Recipe getRecipe(Long number) {
        Recipe recipe = new Recipe();
        recipe.setName("somename" + number);
        recipe.setCreator("someone");
        recipe.setContent("bnllaGVoZSwgbmEgZXogZWd5IGZhc3phIGJhc2U2NCBjdWNj");
        recipe.setId(number);
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

    private Recipe recipeWithType(String name, RecipeType type) {
        Recipe recipe = new Recipe();
        recipe.setRecipeType(type);
        recipe.setName(name);
        return recipe;
    }

    private HostGroup hostGroup(String name, Set<Recipe> recipes) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setRecipes(recipes);
        hostGroup.setName(name);
        return hostGroup;
    }

    private Stack stack(Set<HostGroup> hostGroups) {
        Stack stack = new Stack();
        stack.setResourceCrn("crn");
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        cluster.setHostGroups(hostGroups);
        return stack;
    }

}