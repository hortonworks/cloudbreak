package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class RecipeEngineTest {

    private static final Long DUMMY_STACK_ID = 1L;

    @InjectMocks
    private RecipeEngine recipeEngine;

    @Mock
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private RecipeTemplateService recipeTemplateService;

    @BeforeEach
    public void setUp() {
        recipeEngine = new RecipeEngine();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUploadRecipes() throws CloudbreakException {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(eq(DUMMY_STACK_ID))).willReturn(stack());
        given(hostGroupService.getByClusterWithRecipes(eq(DUMMY_STACK_ID))).willReturn(hostGroups());
        // WHEN
        recipeEngine.uploadRecipes(DUMMY_STACK_ID);
        // THEN
        verify(orchestratorRecipeExecutor, times(1)).uploadRecipes(any(Stack.class), anyMap());
        verify(recipeTemplateService, times(1)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testUploadUpscaleRecipes() throws CloudbreakException {
        // GIVEN
        // WHEN
        recipeEngine.uploadUpscaleRecipes(stack(), Set.of(masterHostGroup()), hostGroups());
        // THEN
        verify(orchestratorRecipeExecutor, times(1)).uploadRecipes(any(Stack.class), anyMap());
        verify(recipeTemplateService, times(1)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testUploadUpscaleRecipesWithoutRecipe() throws CloudbreakException {
        // GIVEN
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName("worker");
        // WHEN
        recipeEngine.uploadUpscaleRecipes(stack(), Set.of(hostGroup), hostGroups());
        // THEN
        verify(orchestratorRecipeExecutor, times(0)).uploadRecipes(any(Stack.class), anyMap());
        verify(recipeTemplateService, times(0)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testExecutePreTerminationRecipes() throws CloudbreakException {
        // GIVEN
        given(hostGroupService.getRecipesByHostGroups(anySet())).willReturn(Set.of(recipe()));
        given(recipeTemplateService.hasAnyTemplateInRecipes(anySet())).willReturn(false);
        given(recipeTemplateService.isGeneratedRecipesInDbStale(anySet(), anyMap())).willReturn(false);
        // WHEN
        recipeEngine.executePreTerminationRecipes(stack(), hostGroups(), false);
        // THEN
        verify(recipeTemplateService, times(1)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testExecutePreTerminationRecipesWithTemplate() throws CloudbreakException {
        // GIVEN
        given(hostGroupService.getRecipesByHostGroups(anySet())).willReturn(Set.of(recipe()));
        given(recipeTemplateService.hasAnyTemplateInRecipes(anySet())).willReturn(true);
        // WHEN
        recipeEngine.executePreTerminationRecipes(stack(), hostGroups(), false);
        // THEN
        verify(recipeTemplateService, times(0)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testExecutePostInstallRecipes() throws CloudbreakException {
        // GIVEN
        Recipe recipe = new Recipe();
        recipe.setRecipeType(RecipeType.POST_CLUSTER_INSTALL);
        given(hostGroupService.getRecipesByHostGroups(anySet())).willReturn(Set.of(recipe));
        given(recipeTemplateService.isGeneratedRecipesInDbStale(anySet(), anyMap())).willReturn(false);
        // WHEN
        recipeEngine.executePostInstallRecipes(stack(), hostGroups());
        // THEN
        verify(recipeTemplateService, times(1)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    @Test
    public void testGetRecipeNameMapWithDuplications() throws CloudbreakException {
        // GIVEN
        Recipe recipe = new Recipe();
        recipe.setName("myrecipe");
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hg1 = new HostGroup();
        hg1.setRecipes(Set.of(recipe));
        hg1.setName("master");
        HostGroup hg2 = new HostGroup();
        hg2.setRecipes(Set.of(recipe));
        hg2.setName("worker");
        hgs.add(hg1);
        hgs.add(hg2);
        given(stackService.getByIdWithListsInTransaction(eq(DUMMY_STACK_ID))).willReturn(stack());
        given(hostGroupService.getByClusterWithRecipes(eq(DUMMY_STACK_ID))).willReturn(hgs);
        // WHEN
        recipeEngine.uploadRecipes(DUMMY_STACK_ID);
        // THEN
        verify(orchestratorRecipeExecutor, times(1)).uploadRecipes(any(Stack.class), anyMap());
        verify(recipeTemplateService, times(1)).updateAllGeneratedRecipes(anySet(), anyMap());
    }

    private Stack stack() {
        Stack stack = new Stack();
        stack.setId(DUMMY_STACK_ID);
        stack.setName("dummy");
        Cluster cluster = new Cluster();
        cluster.setId(DUMMY_STACK_ID);
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setId(DUMMY_STACK_ID);
        stack.setWorkspace(workspace);
        return stack;
    }

    private Set<HostGroup> hostGroups() {
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgWorker = new HostGroup();
        hgWorker.setName("worker");
        hgs.add(hgWorker);
        hgs.add(masterHostGroup());
        return hgs;
    }

    private HostGroup masterHostGroup() {
        HostGroup hgMaster = new HostGroup();
        hgMaster.setName("master");
        hgMaster.setRecipes(Set.of(recipe()));
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        hgMaster.setInstanceGroup(instanceGroup);
        return hgMaster;
    }

    private Recipe recipe() {
        Recipe recipe = new Recipe();
        recipe.setContent("dummy");
        recipe.setName("dummy");
        recipe.setRecipeType(RecipeType.PRE_TERMINATION);
        return recipe;
    }

}
