package com.sequenceiq.cloudbreak.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class RecipeCrnListProviderServiceTest {

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @InjectMocks
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @Test
    public void testGetResourceCrnListByResourceNameList() {
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeResponse1 = new RecipeViewV4Response();
        recipeResponse1.setName("recipe1");
        recipeResponse1.setCrn("crn1");
        RecipeViewV4Response recipeResponse2 = new RecipeViewV4Response();
        recipeResponse2.setName("recipe2");
        recipeResponse2.setCrn("crn2");
        recipeViewV4Responses.setResponses(Set.of(recipeResponse1, recipeResponse2));
        when(recipeV4Endpoint.list(any())).thenReturn(recipeViewV4Responses);
        List<String> resourceCrnListByResourceNameList = recipeCrnListProviderService.getResourceCrnListByResourceNameList(List.of("recipe1", "recipe2"));
        assertThat(resourceCrnListByResourceNameList).containsExactlyInAnyOrder("crn1", "crn2");
    }

    @Test
    public void testGetResourceCrnListByResourceNameListButOnlyOneRecipeWasFound() {
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeResponse1 = new RecipeViewV4Response();
        recipeResponse1.setName("recipe1");
        recipeResponse1.setCrn("crn1");
        recipeViewV4Responses.setResponses(Set.of(recipeResponse1));
        when(recipeV4Endpoint.list(any())).thenReturn(recipeViewV4Responses);
        NotFoundException cloudbreakRuntimeException = Assertions.assertThrows(NotFoundException.class,
                () -> recipeCrnListProviderService.getResourceCrnListByResourceNameList(List.of("recipe1", "recipe2")));
        Assertions.assertEquals("Following recipes does not exist: [recipe2]", cloudbreakRuntimeException.getMessage());
    }

}