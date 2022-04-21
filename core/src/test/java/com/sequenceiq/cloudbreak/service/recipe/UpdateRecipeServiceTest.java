package com.sequenceiq.cloudbreak.service.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.UpdateHostGroupRecipes;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@ExtendWith(MockitoExtension.class)
public class UpdateRecipeServiceTest {

    private static final Long DUMMY_ID = 1L;

    private static final String PRE_CLDR_START_RECIPE = "pre-cldr-start";

    private static final String POST_CLDR_START_RECIPE = "post-cldr-start";

    private  static final String MASTER_HOST_GROUP_NAME = "master";

    private static final String GATEWAY_HOST_GROUP_NAME = "gateway";

    @InjectMocks
    private UpdateRecipeService underTest;

    @Mock
    private RecipeService recipeService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private GeneratedRecipeService generatedRecipeService;

    @BeforeEach
    public void setUp() {
        underTest = new UpdateRecipeService(recipeService, hostGroupService, generatedRecipeService);
    }

    @Test
    public void testRefreshRecipesForCluster() {
        // GIVEN
        Map<String, Set<String>> sampleMap = new HashMap<>();
        sampleMap.put(MASTER_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        Map<String, Set<String>> hostGroupsSample = new HashMap<>();
        hostGroupsSample.put(MASTER_HOST_GROUP_NAME, Set.of(POST_CLDR_START_RECIPE));
        when(recipeService.getByNamesForWorkspaceId(anySet(), anyLong()))
                .thenReturn(createRecipes(Set.of(PRE_CLDR_START_RECIPE, POST_CLDR_START_RECIPE)));
        when(hostGroupService.getByClusterWithRecipes(anyLong()))
                .thenReturn(createHostGroupWithRecipes(hostGroupsSample));
        doNothing().when(generatedRecipeService).deleteAll(anySet());
        // WHEN
        UpdateRecipesV4Response response = underTest.refreshRecipesForCluster(DUMMY_ID, createStack(), createUpdateHostGroupRecipes(sampleMap));
        // THEN
        assertTrue(response.getRecipesAttached().get(0).getRecipeNames().contains(PRE_CLDR_START_RECIPE));
        verify(generatedRecipeService, times(1)).deleteAll(anySet());
    }

    @Test
    public void testRefreshRecipesForClusterNoUpdate() {
        // GIVEN
        Map<String, Set<String>> sampleMap = new HashMap<>();
        sampleMap.put(MASTER_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        Map<String, Set<String>> hostGroupsSample = new HashMap<>();
        hostGroupsSample.put(MASTER_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        when(recipeService.getByNamesForWorkspaceId(anySet(), anyLong()))
                .thenReturn(createRecipes(Set.of(PRE_CLDR_START_RECIPE)));
        when(hostGroupService.getByClusterWithRecipes(anyLong()))
                .thenReturn(createHostGroupWithRecipes(hostGroupsSample));
        // WHEN
        UpdateRecipesV4Response response = underTest.refreshRecipesForCluster(DUMMY_ID, createStack(), createUpdateHostGroupRecipes(sampleMap));
        // THEN
        assertTrue(response.getRecipesAttached().isEmpty());
        assertTrue(response.getRecipesDetached().isEmpty());
    }

    @Test
    public void testRefreshRecipesForClusterRecipeDoesNotExist() {
        // GIVEN
        Map<String, Set<String>> sampleMap = new HashMap<>();
        sampleMap.put(MASTER_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        when(recipeService.getByNamesForWorkspaceId(anySet(), anyLong()))
                .thenReturn(createRecipes(Set.of(POST_CLDR_START_RECIPE)));
        // WHEN
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.refreshRecipesForCluster(DUMMY_ID, createStack(),
                createUpdateHostGroupRecipes(sampleMap)));
        // THEN
        assertEquals("Following recipes do not exist in workspace: pre-cldr-start", exception.getMessage());
    }

    @Test
    public void testRefreshRecipesForClusterAttachAndDetach() {
        // GIVEN
        Map<String, Set<String>> sampleMap = new HashMap<>();
        sampleMap.put(MASTER_HOST_GROUP_NAME, new HashSet<>());
        sampleMap.put(GATEWAY_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        Map<String, Set<String>> hostGroupsSample = new HashMap<>();
        hostGroupsSample.put(MASTER_HOST_GROUP_NAME, Set.of(POST_CLDR_START_RECIPE));
        hostGroupsSample.put(GATEWAY_HOST_GROUP_NAME, new HashSet<>());
        when(recipeService.getByNamesForWorkspaceId(anySet(), anyLong()))
                .thenReturn(createRecipes(Set.of(PRE_CLDR_START_RECIPE, POST_CLDR_START_RECIPE)));
        when(hostGroupService.getByClusterWithRecipes(anyLong()))
                .thenReturn(createHostGroupWithRecipes(hostGroupsSample));
        // WHEN
        UpdateRecipesV4Response response = underTest.refreshRecipesForCluster(DUMMY_ID, createStack(),
                createUpdateHostGroupRecipes(sampleMap));
        // THEN
        assertTrue(response.getRecipesAttached().get(0).getRecipeNames().contains(PRE_CLDR_START_RECIPE));
        assertTrue(response.getRecipesDetached().get(0).getRecipeNames().contains(POST_CLDR_START_RECIPE));
    }

    @Test
    public void testRefreshRecipesForClusterOnlyDbUpdate() {
        // GIVEN
        Map<String, Set<String>> sampleMap = new HashMap<>();
        sampleMap.put(MASTER_HOST_GROUP_NAME, Set.of(PRE_CLDR_START_RECIPE));
        Map<String, Set<String>> hostGroupsSample = new HashMap<>();
        hostGroupsSample.put(MASTER_HOST_GROUP_NAME, Set.of(POST_CLDR_START_RECIPE));
        when(recipeService.getByNamesForWorkspaceId(anySet(), anyLong()))
                .thenReturn(createRecipes(Set.of(PRE_CLDR_START_RECIPE, POST_CLDR_START_RECIPE)));
        when(hostGroupService.getByClusterWithRecipes(anyLong()))
                .thenReturn(createHostGroupWithRecipes(hostGroupsSample));
        // WHEN
        UpdateRecipesV4Response response = underTest.refreshRecipesForCluster(DUMMY_ID, createStack(),
                createUpdateHostGroupRecipes(sampleMap));
        // THEN
        assertTrue(response.getRecipesAttached().get(0).getRecipeNames().contains(PRE_CLDR_START_RECIPE));
    }

    @Test
    public void testDetachRecipeFromCluster() {
        // GIVEN
        Set<Recipe> recipes = createRecipes(Set.of(POST_CLDR_START_RECIPE));
        Map<String, Set<String>> hostGroupsSample = new HashMap<>();
        hostGroupsSample.put(MASTER_HOST_GROUP_NAME, Set.of(POST_CLDR_START_RECIPE));
        Set<HostGroup> hostGroups = createHostGroupWithRecipes(hostGroupsSample);
        Recipe sampleRecipe = recipes.stream().findFirst().orElse(null);
        HostGroup sampleHostGroup = hostGroups.stream().findFirst().orElse(null);
        given(hostGroupService.getByClusterIdAndNameWithRecipes(anyLong(), anyString()))
                .willReturn(sampleHostGroup);
        given(recipeService.getByNameForWorkspaceId(anyString(), anyLong()))
                .willReturn(sampleRecipe);
        given(hostGroupService.save(any())).willReturn(sampleHostGroup);
        doNothing().when(generatedRecipeService).deleteAll(anySet());
        // WHEN
        underTest.detachRecipeFromCluster(DUMMY_ID, createStack(), POST_CLDR_START_RECIPE, MASTER_HOST_GROUP_NAME);
        // THEN
        verify(hostGroupService, times(1)).save(any());
        verify(hostGroupService, times(1)).getByClusterIdAndNameWithRecipes(anyLong(), anyString());
        verify(generatedRecipeService, times(1)).deleteAll(anySet());
    }

    private List<UpdateHostGroupRecipes> createUpdateHostGroupRecipes(Map<String, Set<String>> hostGroupRecipesMap) {
        List<UpdateHostGroupRecipes> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : hostGroupRecipesMap.entrySet()) {
            UpdateHostGroupRecipes item = new UpdateHostGroupRecipes();
            item.setHostGroupName(entry.getKey());
            item.setRecipeNames(entry.getValue());
            result.add(item);
        }
        return result;
    }

    private Set<HostGroup> createHostGroupWithRecipes(Map<String, Set<String>> hostGroupRecipesMap) {
        Set<HostGroup> result = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : hostGroupRecipesMap.entrySet()) {
            HostGroup hostGroup = new HostGroup();
            hostGroup.setName(entry.getKey());
            Set<Recipe> recipeSet = new HashSet<>();
            Set<GeneratedRecipe> generatedRecipeSet = new HashSet<>();
            for (String recipeName : entry.getValue()) {
                Recipe recipe = new Recipe();
                GeneratedRecipe generatedRecipe = new GeneratedRecipe();
                generatedRecipe.setRecipe(recipe);
                recipe.setName(recipeName);
                recipe.setGeneratedRecipes(Set.of(generatedRecipe));
                recipeSet.add(recipe);
                generatedRecipeSet.add(generatedRecipe);
            }
            hostGroup.setRecipes(recipeSet);
            hostGroup.setGeneratedRecipes(generatedRecipeSet);
            result.add(hostGroup);
        }
        return result;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(DUMMY_ID);
        Cluster cluster = new Cluster();
        cluster.setId(DUMMY_ID);
        stack.setCluster(cluster);
        return stack;
    }

    private Set<Recipe> createRecipes(Set<String> recipeNames) {
        Set<Recipe> recipeSet = new HashSet<>();
        for (String recipeName : recipeNames) {
            Recipe recipe = new Recipe();
            recipe.setName(recipeName);
            recipeSet.add(recipe);
        }
        return recipeSet;
    }
}
