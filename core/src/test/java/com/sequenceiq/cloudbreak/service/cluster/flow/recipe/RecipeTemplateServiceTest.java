package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.recipe.GeneratedRecipeService;

@ExtendWith(MockitoExtension.class)
public class RecipeTemplateServiceTest {

    private static final String DUMMY_CONTENT_1 = "echo hello1";

    private static final String DUMMY_CONTENT_2 = "echo hello2";

    @InjectMocks
    private RecipeTemplateService recipeTemplateService;

    @Mock
    private GeneratedRecipeService generatedRecipeService;

    @Mock
    private CentralRecipeUpdater centralRecipeUpdater;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @BeforeEach
    public void setUp() {
        recipeTemplateService = new RecipeTemplateService(generatedRecipeService, centralRecipeUpdater,
                stackToTemplatePreparationObjectConverter, transactionService);
    }

    @Test
    public void testCompareGenerateRecipesMatches() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1)));
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_1)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCompareGenerateRecipesWithNotMatchingGeneratedReceipe() {
        // GIVEN
        Recipe recipe = recipe("r1", DUMMY_CONTENT_2);
        Set<HostGroup> hgs = new HashSet<>();
        Set<GeneratedRecipe> generatedRecipes = new HashSet<>();
        GeneratedRecipe generatedRecipe = new GeneratedRecipe();
        generatedRecipe.setExtendedRecipeText(DUMMY_CONTENT_1);
        generatedRecipe.setRecipe(recipe);
        generatedRecipes.add(generatedRecipe);
        HostGroup hgMaster = hostGroup("master", Set.of(recipe), generatedRecipes);
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_2)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCompareGenerateRecipesWithGeneratedRecipeButNoRecipe() {
        // GIVEN
        Recipe recipe = recipe("r1", DUMMY_CONTENT_2);
        Set<HostGroup> hgs = new HashSet<>();
        Set<GeneratedRecipe> generatedRecipes = new HashSet<>();
        GeneratedRecipe generatedRecipe = new GeneratedRecipe();
        generatedRecipe.setExtendedRecipeText(DUMMY_CONTENT_1);
        generatedRecipe.setRecipe(recipe);
        generatedRecipes.add(generatedRecipe);
        HostGroup hgMaster = hostGroup("master", new HashSet<>(), generatedRecipes);
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCompareGenerateRecipesWithNotMatchingModel() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1)));
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_2)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCompareGenerateRecipesDifferentSize() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1), recipe("r2", DUMMY_CONTENT_1)));
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_1)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCompareGenerateRecipesWithoutGeneratedRecipes() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1)), new HashSet<>());
        hgs.add(hgMaster);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_1)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCompareGenerateRecipesWithoutRecipes() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", new HashSet<>(), new HashSet<>());
        hgs.add(hgMaster);
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, new HashMap<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCompareGenerateRecipesWithoutHostGroups() {
        // GIVEN
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(new HashSet<>(), new HashMap<>());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCompareGenerateRecipesWithMultipleHostGroups() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        HostGroup hgMaster = hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1), recipe("r2", DUMMY_CONTENT_2)));
        HostGroup hgGateway = hostGroup("gateway", new HashSet<>(), new HashSet<>());
        HostGroup hgWorker = hostGroup("worker", Set.of(recipe("r2", DUMMY_CONTENT_2)));
        hgs.add(hgMaster);
        hgs.add(hgGateway);
        hgs.add(hgWorker);
        Map<HostGroup, List<RecipeModel>> recipeModels = new HashMap<>();
        recipeModels.put(hgMaster, List.of(
                new RecipeModel("r1", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_1),
                new RecipeModel("r2", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_2)));
        recipeModels.put(hgWorker, List.of(new RecipeModel("r2", RecipeType.PRE_CLOUDERA_MANAGER_START, DUMMY_CONTENT_2)));
        // WHEN
        boolean result = recipeTemplateService.compareGeneratedRecipes(hgs, recipeModels);
        // THEN
        assertTrue(result);
    }

    private HostGroup hostGroup(String name, Set<Recipe> recipes, Set<GeneratedRecipe> generatedRecipes) {
        HostGroup hg = new HostGroup();
        hg.setName(name);
        hg.setRecipes(recipes);
        hg.setGeneratedRecipes(generatedRecipes);
        return hg;
    }

    private HostGroup hostGroup(String name, Set<Recipe> recipes) {
        Set<GeneratedRecipe> generatedRecipes = new HashSet<>();
        for (Recipe recipe : recipes) {
            GeneratedRecipe generatedRecipe = new GeneratedRecipe();
            generatedRecipe.setRecipe(recipe);
            generatedRecipe.setExtendedRecipeText(recipe.getContent());
            generatedRecipes.add(generatedRecipe);
        }
        return hostGroup(name, recipes, generatedRecipes);
    }

    private Recipe recipe(String name, String content) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setContent(content);
        return recipe;
    }
}
