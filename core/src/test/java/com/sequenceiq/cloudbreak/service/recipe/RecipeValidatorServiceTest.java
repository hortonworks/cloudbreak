package com.sequenceiq.cloudbreak.service.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class RecipeValidatorServiceTest {

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private RecipeValidatorService recipeValidatorService;

    @Test
    void testValidateRecipeExistenceOnInstanceGroups() {
        List<InstanceGroupV4Request> instanceGroupRequests = List.of(
                createInstanceGroupRequest("Group1", "Recipe1", "Recipe2"),
                createInstanceGroupRequest("Group2", "Recipe3", "Recipe4")
        );

        Set<String> namesInTheDb = Set.of("Recipe1", "Recipe2", "Recipe3");

        when(recipeService.getResourceNamesByNamesAndWorkspaceId(anyList(), anyLong())).thenReturn(namesInTheDb);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> recipeValidatorService.validateRecipeExistenceOnInstanceGroups(instanceGroupRequests, 123L));

        assertEquals(exception.getMessage(), "The given recipe does not exist for the instance group \"Group2\": Recipe4");
    }

    @Test
    void testValidateRecipeExistenceOnInstanceGroupsWithMissingRecipes() {
        List<InstanceGroupV4Request> instanceGroupRequests = List.of(
                createInstanceGroupRequest("Group1", "Recipe1", "Recipe2", "Recipe3", "Recipe4")
        );

        Set<String> namesInTheDb = Set.of("Recipe1", "Recipe2");

        when(recipeService.getResourceNamesByNamesAndWorkspaceId(anyList(), anyLong())).thenReturn(namesInTheDb);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> recipeValidatorService.validateRecipeExistenceOnInstanceGroups(instanceGroupRequests, 123L));

        assertEquals(exception.getMessage(), "The given recipes do not exist for the instance group \"Group1\": Recipe3, Recipe4");
    }

    @Test
    void testValidateRecipeExistenceWhenEveryRecipePresentedInTheDatabase() {
        List<InstanceGroupV4Request> instanceGroupRequests = List.of(
                createInstanceGroupRequest("Group1", "Recipe1", "Recipe2"),
                createInstanceGroupRequest("Group2", "Recipe3", "Recipe4")
        );

        Set<String> namesInTheDb = Set.of("Recipe1", "Recipe2", "Recipe3", "Recipe4");

        when(recipeService.getResourceNamesByNamesAndWorkspaceId(anyList(), anyLong())).thenReturn(namesInTheDb);

        recipeValidatorService.validateRecipeExistenceOnInstanceGroups(instanceGroupRequests, 123L);
    }

    private InstanceGroupV4Request createInstanceGroupRequest(String name, String... recipeNames) {
        InstanceGroupV4Request instanceGroupRequest = new InstanceGroupV4Request();
        instanceGroupRequest.setName(name);
        instanceGroupRequest.setRecipeNames(Set.of(recipeNames));
        return instanceGroupRequest;
    }
}