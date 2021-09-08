package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

public class RecipeHashServiceTest {

    private static final String DUMMY_CONTENT_1 = "echo hello1";

    private static final String DUMMY_CONTENT_2 = "echo hello2";

    private RecipeHashService recipeHashService;

    @BeforeEach
    public void setUp() {
        recipeHashService = new RecipeHashService();
    }

    @Test
    public void testCheckRecipeHashes() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_1)), Map.of("r1", sha256(DUMMY_CONTENT_1))));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckRecipeHashesWithNewRecipe() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(recipe("r2", DUMMY_CONTENT_1)), Map.of("r1", sha256(DUMMY_CONTENT_1))));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCheckRecipeHashesWithOldRecipeHash() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_2)), Map.of("r1", sha256(DUMMY_CONTENT_1))));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCheckRecipeHashesWithNoRecipeHash() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(recipe("r1", DUMMY_CONTENT_2)), null));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCheckRecipeHashesWithEmptyHashesAndRecipes() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(), Map.of()));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckRecipeHashesWithNoRecipeHashAndNoRecipes() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(), null));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckRecipeHashesWithMultipleHostGroups() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(), null));
        hgs.add(hostGroup("gateway", Set.of(recipe("r1", DUMMY_CONTENT_2)), Map.of("r1", sha256(DUMMY_CONTENT_2))));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckRecipeHashesWithMultipleHostGroupsWithOldRecipeHash() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(), null));
        hgs.add(hostGroup("gateway", Set.of(recipe("r1", DUMMY_CONTENT_2)), Map.of("r1", sha256(DUMMY_CONTENT_1))));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCheckRecipeHashesWithNoRecipeHashAndNoRecipesAndMultipleHostGroups() {
        // GIVEN
        Set<HostGroup> hgs = new HashSet<>();
        hgs.add(hostGroup("master", Set.of(), null));
        hgs.add(hostGroup("gateway", Set.of(recipe("r1", DUMMY_CONTENT_1)), null));
        // WHEN
        boolean result = recipeHashService.checkRecipeHashes(hgs);
        // THEN
        assertFalse(result);
    }

    private HostGroup hostGroup(String name, Set<Recipe> recipes, Map<String, String> hashes) {
        HostGroup hg = new HostGroup();
        hg.setName(name);
        hg.setRecipes(recipes);
        hg.setRecipeHashes(hashes);
        return hg;
    }

    private Recipe recipe(String name, String content) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setContent(content);
        return recipe;
    }

    private String sha256(String text) {
        return DigestUtils.sha256Hex(text);
    }
}
