package com.sequenceiq.environment.environment.service.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.environment.environment.domain.FreeIpaRecipe;
import com.sequenceiq.environment.environment.repository.FreeIpaRecipeRepository;

@ExtendWith(MockitoExtension.class)
class EnvironmentRecipeServiceTest {

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @Mock
    private FreeIpaRecipeRepository freeIpaRecipeRepository;

    @Mock
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @InjectMocks
    private EnvironmentRecipeService environmentRecipeService;

    @Test
    public void testGetResourceCrnListByResourceNameList() {
        List<String> recipes = List.of("recipe1", "recipe2");
        List<String> crns = List.of("crn1", "crn2");
        when(recipeCrnListProviderService.getResourceCrnListByResourceNameList(recipes)).thenReturn(crns);
        List<String> resourceCrnListByResourceNameList = environmentRecipeService.getResourceCrnListByResourceNameList(recipes);
        assertEquals(crns, resourceCrnListByResourceNameList);
    }

    @Test
    void testGetRecipes() {
        List<FreeIpaRecipe> freeIpaRecipes = List.of(new FreeIpaRecipe(1L, "recipe1"), new FreeIpaRecipe(1L, "recipe2"));
        when(freeIpaRecipeRepository.findByEnvironmentId(1L)).thenReturn(freeIpaRecipes);
        Set<String> recipesForEnvironment = environmentRecipeService.getRecipes(1L);
        assertThat(recipesForEnvironment).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    void testSaveRecipes() {
        environmentRecipeService.saveRecipes(Set.of("recipe1", "recipe2"), 1L);
        ArgumentCaptor<Iterable<FreeIpaRecipe>> iterableArgumentCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(freeIpaRecipeRepository, times(1)).saveAll(iterableArgumentCaptor.capture());
        List<FreeIpaRecipe> freeIpaRecipes = StreamSupport.stream(iterableArgumentCaptor.getValue().spliterator(), false)
                .collect(Collectors.toList());
        assertThat(freeIpaRecipes.stream().map(FreeIpaRecipe::getRecipe)).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    void testDeleteRecipes() {
        environmentRecipeService.deleteRecipes(1L);
        verify(freeIpaRecipeRepository, times(1)).deleteFreeIpaRecipesByEnvironmentId(1L);
    }

}