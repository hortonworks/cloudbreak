package com.sequenceiq.freeipa.service.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;
import com.sequenceiq.freeipa.repository.FreeIpaStackRecipeRepository;

@ExtendWith(MockitoExtension.class)
class FreeIpaRecipeServiceTest {

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @Mock
    private FreeIpaStackRecipeRepository freeIpaStackRecipeRepository;

    @Mock
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @InjectMocks
    private FreeIpaRecipeService freeIpaRecipeService;

    @Test
    public void testGetResourceCrnListByResourceNameList() {
        List<String> recipes = List.of("recipe1", "recipe2");
        List<String> crns = List.of("crn1", "crn2");
        when(recipeCrnListProviderService.getResourceCrnListByResourceNameList(recipes)).thenReturn(crns);
        List<String> resourceCrnListByResourceNameList = freeIpaRecipeService.getResourceCrnListByResourceNameList(recipes);
        assertEquals(crns, resourceCrnListByResourceNameList);
    }

    @Test
    public void testGetRecipes() {
        RecipeV4Request recipe1Request = new RecipeV4Request();
        recipe1Request.setName("recipe1");
        recipe1Request.setType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START);
        recipe1Request.setContent("YmFzaDE=");
        RecipeV4Request recipe2Request = new RecipeV4Request();
        recipe2Request.setName("recipe2");
        recipe2Request.setType(RecipeV4Type.PRE_TERMINATION);
        recipe2Request.setContent("YmFzaDI=");
        when(recipeV4Endpoint.getRequest(0L, "recipe1")).thenReturn(recipe1Request);
        when(recipeV4Endpoint.getRequest(0L, "recipe2")).thenReturn(recipe2Request);
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(1L);
        RecipeModel recipeModel1 = recipes.stream().filter(recipeModel -> "recipe1".equals(recipeModel.getName())).findFirst().get();
        RecipeModel recipeModel2 = recipes.stream().filter(recipeModel -> "recipe2".equals(recipeModel.getName())).findFirst().get();
        Assertions.assertEquals(RecipeType.PRE_CLOUDERA_MANAGER_START, recipeModel1.getRecipeType());
        Assertions.assertEquals(RecipeType.PRE_TERMINATION, recipeModel2.getRecipeType());
        Assertions.assertEquals("bash1", recipeModel1.getGeneratedScript());
        Assertions.assertEquals("bash2", recipeModel2.getGeneratedScript());
    }

    @Test
    void testSaveRecipes() {
        freeIpaRecipeService.saveRecipes(Set.of("recipe1", "recipe2"), 1L);
        ArgumentCaptor<Iterable<FreeIpaStackRecipe>> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(freeIpaStackRecipeRepository, times(1)).saveAll(iterableArgumentCaptor.capture());
        List<FreeIpaStackRecipe> freeIpaRecipes = StreamSupport.stream(iterableArgumentCaptor.getValue().spliterator(), false)
                .collect(Collectors.toList());
        assertThat(freeIpaRecipes.stream().map(FreeIpaStackRecipe::getRecipe)).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    void testDeleteRecipes() {
        freeIpaRecipeService.deleteRecipes(1L);
        verify(freeIpaStackRecipeRepository, times(1)).deleteFreeIpaStackRecipesByStackId(1L);
    }

}