package com.sequenceiq.cloudbreak.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
@Disabled("Will be fixed in a followup PR -> CB-5659")
class StackCreatorServiceRecipeValidationTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String WORKSPACE_NAME = "myAwesomeWorkspace";

    private static final Long DOMAIN_ID_FOR_USER = 1L;

    private static final String USER_ID = "someIdStuff";

    private static final String INSTANCE_GROUP_MASTER = "master";

    private static final String INSTANCE_GROUP_COMPUTE = "compute";

    @Mock
    private StackService stackService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private Validator<StackV4Request> stackRequestValidator;

    @Mock
    private TransactionService transactionService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakMetricService metricService;

    @Mock
    private RecipeService recipeService;

    @Mock
    private StackV4RequestToStackConverter stackV4RequestToStackConverter;

    @Mock
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @InjectMocks
    private StackCreatorService underTest;

    private User user;

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        workspace = TestUtil.workspace(WORKSPACE_ID, WORKSPACE_NAME);
        user = TestUtil.user(DOMAIN_ID_FOR_USER, USER_ID);

        ValidationResult validationResult = mock(ValidationResult.class);
        when(validationResult.getState()).thenReturn(ValidationResult.State.VALID);
        when(stackRequestValidator.validate(any(StackV4Request.class))).thenReturn(validationResult);
    }

    @Test
    void testIfOneRecipeDoesNotExistsWhichIsGivenInOneOfTheHostgroupsThenBadRequestExceptionShouldCome() {
        String notExistingRecipeName = "someNotExistingRecipe";

        StackV4Request request = new StackV4Request();
        request.setInstanceGroups(List.of(getInstanceGroupWithRecipe(INSTANCE_GROUP_MASTER, Set.of(notExistingRecipeName))));

        when(recipeService.get(any(NameOrCrn.class), eq(WORKSPACE_ID))).thenThrow(new NotFoundException("Recipe not found"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.createStack(user, workspace, request, false));

        assertNotNull(exception);
        assertEquals(String.format("The given recipe does not exist for the instance group \"%s\": %s", INSTANCE_GROUP_MASTER,
                notExistingRecipeName), exception.getMessage());

        verify(recipeService, times(1)).get(any(NameOrCrn.class), anyLong());
    }

    @Test
    void testIfMultipleRecipesDoesNotExistsWhichHasGivenInOneOfTheHostgroupsThenBadRequestExceptionShouldCome() {
        String notExistingRecipeName = "someNotExistingRecipe";
        String someOtherNotExistingRecipeName = "someOtherNotExistingRecipe";

        StackV4Request request = new StackV4Request();
        request.setInstanceGroups(List.of(getInstanceGroupWithRecipe(INSTANCE_GROUP_MASTER, Set.of(notExistingRecipeName, someOtherNotExistingRecipeName))));

        when(recipeService.get(any(NameOrCrn.class), eq(WORKSPACE_ID))).thenThrow(new NotFoundException("Recipe not found"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.createStack(user, workspace, request, false));

        assertNotNull(exception);
        assertTrue(exception.getMessage()
                .matches(String.format("The given recipes does not exists for the instance group \"%s\": (\\w+), (\\w+)", INSTANCE_GROUP_MASTER)));

        verify(recipeService, times(2)).get(any(NameOrCrn.class), anyLong());
    }

    @Test
    void testIfOneOfTheRecipeDoesNotExistsInOneInstanceGroupWhenOtherInstanceGroupHaveAnExistingRecipeThenBadRequestComesForThatInstanceGroup() {
        String existingRecipe = "someExistingRecipe";
        String someOtherExistingRecipe = "someOtherExistingRecipe";
        String notExistingRecipe = "thisOneDoesNotExists";

        StackV4Request request = new StackV4Request();
        request.setInstanceGroups(List.of(
                getInstanceGroupWithRecipe(INSTANCE_GROUP_MASTER, Set.of(existingRecipe)),
                getInstanceGroupWithRecipe(INSTANCE_GROUP_COMPUTE, Set.of(someOtherExistingRecipe, notExistingRecipe))));

        doAnswer(withNotFoundExceptionIfRecipeNameMatchesOtherwiseGiveRecipe(notExistingRecipe))
                .when(recipeService).get(any(NameOrCrn.class), eq(WORKSPACE_ID));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.createStack(user, workspace, request, false));

        assertNotNull(exception);
        assertEquals(String.format("The given recipe does not exist for the instance group \"%s\": %s", INSTANCE_GROUP_COMPUTE, notExistingRecipe),
                exception.getMessage());

        verify(recipeService, times(3)).get(any(NameOrCrn.class), anyLong());
    }

    @Test
    void testIfRecipeExistsThenEverythingShouldBeFine() throws TransactionExecutionException {
        String existingRecipeName = "existingRecipe";

        StackV4Request request = new StackV4Request();
        request.setName("stack_name");
        request.setInstanceGroups(List.of(getInstanceGroupWithRecipe(INSTANCE_GROUP_MASTER, Set.of(existingRecipeName))));

        when(recipeService.get(any(NameOrCrn.class), eq(WORKSPACE_ID))).thenReturn(getRecipeWithName(existingRecipeName));
        when(stackV4RequestToStackConverter.convert(request)).thenReturn(TestUtil.stack());
        when(transactionService.required(any(Supplier.class))).thenReturn(TestUtil.stack());
        when(stackService.getIdByNameInWorkspace(anyString(), any(Long.class))).thenThrow(new NotFoundException("stack not found by name"));
        underTest.createStack(user, workspace, request, false);

        verify(recipeService, times(1)).get(any(NameOrCrn.class), anyLong());
    }

    @Test
    void voidTestIfRecipeDoesNotExistInInstanceGroupV4RequestThenEverythingShouldGoFine() throws TransactionExecutionException {
        StackV4Request request = new StackV4Request();
        request.setName("stack_name");
        request.setInstanceGroups(List.of(getInstanceGroupWithRecipe(INSTANCE_GROUP_MASTER, null),
                getInstanceGroupWithRecipe(INSTANCE_GROUP_COMPUTE, null)));

        when(stackV4RequestToStackConverter.convert(request)).thenReturn(TestUtil.stack());
        when(transactionService.required(any(Supplier.class))).thenReturn(TestUtil.stack());
        when(stackService.getIdByNameInWorkspace(anyString(), any(Long.class))).thenThrow(new NotFoundException("stack not found by name"));

        underTest.createStack(user, workspace, request, false);

        verify(recipeService, times(0)).get(any(NameOrCrn.class), anyLong());
    }

    private Answer<Recipe> withNotFoundExceptionIfRecipeNameMatchesOtherwiseGiveRecipe(String recipeNameForMatch) {
        return invocation -> {
            NameOrCrn nameOrCrn = invocation.getArgument(0);
            if (nameOrCrn.getName().equals(recipeNameForMatch)) {
                throw new NotFoundException("Recipe not found!");
            }
            return getRecipeWithName(nameOrCrn.getName());
        };
    }

    private InstanceGroupV4Request getInstanceGroupWithRecipe(String instanceGroupName, Set<String> recipeNames) {
        InstanceGroupV4Request request = new InstanceGroupV4Request();
        request.setName(instanceGroupName);
        request.setRecipeNames(recipeNames);
        return request;
    }

    private Recipe getRecipeWithName(String recipeName) {
        Recipe recipe = TestUtil.recipe();
        recipe.setName(recipeName);
        return recipe;
    }

}
