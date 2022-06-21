package com.sequenceiq.freeipa.service.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
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
        ArgumentCaptor<Set<String>> recipeSet = ArgumentCaptor.forClass(Set.class);
        when(recipeV4Endpoint.getRequestsByNames(anyLong(), recipeSet.capture())).thenReturn(Set.of(recipe1Request, recipe2Request));
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(1L);
        RecipeModel recipeModel1 = recipes.stream().filter(recipeModel -> "recipe1".equals(recipeModel.getName())).findFirst().get();
        RecipeModel recipeModel2 = recipes.stream().filter(recipeModel -> "recipe2".equals(recipeModel.getName())).findFirst().get();
        Assertions.assertEquals(RecipeType.PRE_CLOUDERA_MANAGER_START, recipeModel1.getRecipeType());
        Assertions.assertEquals(RecipeType.PRE_TERMINATION, recipeModel2.getRecipeType());
        Assertions.assertEquals("bash1", recipeModel1.getGeneratedScript());
        Assertions.assertEquals("bash2", recipeModel2.getGeneratedScript());
        assertThat(recipeSet.getValue()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    public void testGetRecipesButOneMissing() {
        RecipeV4Request recipe1Request = new RecipeV4Request();
        recipe1Request.setName("recipe1");
        recipe1Request.setType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START);
        recipe1Request.setContent("YmFzaDE=");
        RecipeV4Request recipe2Request = new RecipeV4Request();
        recipe2Request.setName("recipe2");
        recipe2Request.setType(RecipeV4Type.PRE_TERMINATION);
        recipe2Request.setContent("YmFzaDI=");
        NotFoundException notFoundException = mock(NotFoundException.class);
        Response response = mock(Response.class);
        when(response.readEntity(ExceptionResponse.class)).thenReturn(new ExceptionResponse("recipe2 not found"));
        when(notFoundException.getResponse()).thenReturn(response);
        when(recipeV4Endpoint.getRequestsByNames(eq(0L), anySet())).thenThrow(notFoundException);
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        CloudbreakServiceException cloudbreakServiceException = Assertions.assertThrows(CloudbreakServiceException.class,
                () -> freeIpaRecipeService.getRecipes(1L));
        assertEquals("Missing recipe(s): recipe2 not found", cloudbreakServiceException.getMessage());
    }

    @Test
    public void testGetRecipesButNoRecipeForFreeipa() {
        RecipeV4Request recipe1Request = new RecipeV4Request();
        recipe1Request.setName("recipe1");
        recipe1Request.setType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START);
        recipe1Request.setContent("YmFzaDE=");
        RecipeV4Request recipe2Request = new RecipeV4Request();
        recipe2Request.setName("recipe2");
        recipe2Request.setType(RecipeV4Type.PRE_TERMINATION);
        recipe2Request.setContent("YmFzaDI=");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = Collections.emptyList();
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(1L);
        assertThat(recipes).isEmpty();
        verify(recipeV4Endpoint, times(0)).getRequestsByNames(any(), anySet());
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