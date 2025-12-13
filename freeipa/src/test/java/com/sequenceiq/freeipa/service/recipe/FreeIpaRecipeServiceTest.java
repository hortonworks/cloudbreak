package com.sequenceiq.freeipa.service.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.cloudbreak.usage.service.RecipeUsageService;
import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;
import com.sequenceiq.freeipa.entity.FreeIpaStackRecipe;
import com.sequenceiq.freeipa.repository.FreeIpaStackRecipeRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.RecipeAttachmentChecker;

@ExtendWith(MockitoExtension.class)
class FreeIpaRecipeServiceTest {

    private static final String INTERNAL_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:test@cloudera.com";

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @Mock
    private FreeIpaStackRecipeRepository freeIpaStackRecipeRepository;

    @Mock
    private RecipeCrnListProviderService recipeCrnListProviderService;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private StackService stackService;

    @Mock
    private RecipeUsageService recipeUsageService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RecipeAttachmentChecker recipeAttachmentChecker;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory;

    @InjectMocks
    private FreeIpaRecipeService freeIpaRecipeService;

    @BeforeEach
    public void setup() {
        lenient().doNothing().when(recipeUsageService).sendAttachedUsageReport(anyString(), any(), any(), anyString(), any());
        lenient().doNothing().when(recipeUsageService).sendDetachedUsageReport(anyString(), any(), any(), anyString(), any());
        RegionAwareInternalCrnGenerator internalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        lenient().when(internalCrnGeneratorFactory.iam()).thenReturn(internalCrnGenerator);
        lenient().when(internalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_CRN);
    }

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
        recipe1Request.setType(RecipeV4Type.PRE_SERVICE_DEPLOYMENT);
        recipe1Request.setContent("YmFzaDE=");
        RecipeV4Request recipe2Request = new RecipeV4Request();
        recipe2Request.setName("recipe2");
        recipe2Request.setType(RecipeV4Type.PRE_TERMINATION);
        recipe2Request.setContent("YmFzaDI=");
        ArgumentCaptor<Set<String>> recipeSet = ArgumentCaptor.forClass(Set.class);
        when(recipeV4Endpoint.getRequestsByNames(anyLong(), recipeSet.capture(), any())).thenReturn(Set.of(recipe1Request, recipe2Request));
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(1L);
        RecipeModel recipeModel1 = recipes.stream().filter(recipeModel -> "recipe1".equals(recipeModel.getName())).findFirst().get();
        RecipeModel recipeModel2 = recipes.stream().filter(recipeModel -> "recipe2".equals(recipeModel.getName())).findFirst().get();
        assertEquals(RecipeType.PRE_SERVICE_DEPLOYMENT, recipeModel1.getRecipeType());
        assertEquals(RecipeType.PRE_TERMINATION, recipeModel2.getRecipeType());
        assertEquals("bash1", recipeModel1.getGeneratedScript());
        assertEquals("bash2", recipeModel2.getGeneratedScript());
        assertThat(recipeSet.getValue()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    public void testGetUsedRecipeNamesForAccount() {
        when(stackRepository.findStackIdsByAccountId(any())).thenReturn(List.of(1L, 2L, 3L));
        when(freeIpaStackRecipeRepository.findByStackIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(new FreeIpaStackRecipe(1L, "recipe1"),
                new FreeIpaStackRecipe(2L, "recipe2")));
        List<String> recipes = freeIpaRecipeService.getUsedRecipeNamesForAccount("accountId");
        assertThat(recipes).containsExactlyInAnyOrder("recipe1", "recipe2");
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
        when(recipeV4Endpoint.getRequestsByNames(eq(0L), anySet(), any())).thenThrow(notFoundException);
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
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
        verify(recipeV4Endpoint, times(0)).getRequestsByNames(any(), anySet(), any());
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

    @Test
    void testHasRecipeType() {
        RecipeV4Request recipe1Request = new RecipeV4Request();
        recipe1Request.setName("recipe1");
        recipe1Request.setType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START);
        recipe1Request.setContent("YmFzaDE=");
        RecipeV4Request recipe2Request = new RecipeV4Request();
        recipe2Request.setName("recipe2");
        recipe2Request.setType(RecipeV4Type.PRE_TERMINATION);
        recipe2Request.setContent("YmFzaDI=");
        ArgumentCaptor<Set<String>> recipeSet = ArgumentCaptor.forClass(Set.class);
        when(recipeV4Endpoint.getRequestsByNames(anyLong(), recipeSet.capture(), any())).thenReturn(Set.of(recipe1Request, recipe2Request));
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(1L, "recipe2"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> freeIpaRecipeService.getRecipes(1L));
        RecipeModel recipeModel1 = recipes.stream().filter(recipeModel -> "recipe1".equals(recipeModel.getName())).findFirst().get();
        RecipeModel recipeModel2 = recipes.stream().filter(recipeModel -> "recipe2".equals(recipeModel.getName())).findFirst().get();
        assertEquals(RecipeType.PRE_SERVICE_DEPLOYMENT, recipeModel1.getRecipeType());
        assertEquals(RecipeType.PRE_TERMINATION, recipeModel2.getRecipeType());
        assertEquals("bash1", recipeModel1.getGeneratedScript());
        assertEquals("bash2", recipeModel2.getGeneratedScript());
        boolean hasRecipeType = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                freeIpaRecipeService.hasRecipeType(1L, RecipeType.PRE_TERMINATION));
        assertTrue(hasRecipeType);
        assertThat(recipeSet.getValue()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    void testHasRecipeTypeButHasOnlyPreStartRecipe() {
        RecipeV4Request recipe1Request = new RecipeV4Request();
        recipe1Request.setName("recipe1");
        recipe1Request.setType(RecipeV4Type.PRE_CLOUDERA_MANAGER_START);
        recipe1Request.setContent("YmFzaDE=");
        ArgumentCaptor<Set<String>> recipeSet = ArgumentCaptor.forClass(Set.class);
        when(recipeV4Endpoint.getRequestsByNames(anyLong(), recipeSet.capture(), any())).thenReturn(Set.of(recipe1Request));
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(1L);
        RecipeModel recipeModel1 = recipes.stream().filter(recipeModel -> "recipe1".equals(recipeModel.getName())).findFirst().get();
        assertEquals(RecipeType.PRE_SERVICE_DEPLOYMENT, recipeModel1.getRecipeType());
        assertEquals("bash1", recipeModel1.getGeneratedScript());
        boolean hasRecipeType = freeIpaRecipeService.hasRecipeType(1L, RecipeType.PRE_TERMINATION);
        assertFalse(hasRecipeType);
        assertThat(recipeSet.getValue()).containsExactly("recipe1");
    }

    @Test
    void testAttachRecipesHasExistingRecipes() {
        List<String> recipes = List.of("recipe3", "recipe4");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(2L, "recipe2"));
        String accid = "accid";
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", accid)).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        freeIpaRecipeService.attachRecipes(accid, recipeAttachDetachRequest);
        verify(recipeAttachmentChecker, times(0)).isRecipeAttachmentAvailable(1L);
    }

    @Test
    void testAttachRecipesNoExistingRecipesAndAttachmentNotAvailable() {
        List<String> recipes = List.of("recipe3", "recipe4");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = Collections.emptyList();
        String accid = "accid";
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", accid)).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        when(recipeAttachmentChecker.isRecipeAttachmentAvailable(1L)).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> freeIpaRecipeService.attachRecipes(accid,
                recipeAttachDetachRequest));
        verify(recipeAttachmentChecker, times(1)).isRecipeAttachmentAvailable(1L);
        assertEquals("Recipe attachment is not supported for this FreeIpa, please upgrade it first", badRequestException.getMessage());
    }

    @Test
    void testAttachRecipesNoExistingRecipesAndAttachmentAvailable() {
        List<String> recipes = List.of("recipe3", "recipe4");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = Collections.emptyList();
        String accid = "accid";
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", accid)).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        when(recipeAttachmentChecker.isRecipeAttachmentAvailable(1L)).thenReturn(true);
        freeIpaRecipeService.attachRecipes(accid, recipeAttachDetachRequest);
        verify(recipeAttachmentChecker, times(1)).isRecipeAttachmentAvailable(1L);
    }

    @Test
    void testAttachRecipes() {
        List<String> recipes = List.of("recipe3", "recipe4");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(2L, "recipe2"));
        String accid = "accid";
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", accid)).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        ArgumentCaptor<Collection> validationArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        freeIpaRecipeService.attachRecipes(accid, recipeAttachDetachRequest);
        verify(recipeCrnListProviderService).validateRequestedRecipesExistsByName(validationArgumentCaptor.capture());
        assertThat(validationArgumentCaptor.getValue()).containsExactlyInAnyOrder("recipe3", "recipe4");
        ArgumentCaptor<Set<FreeIpaStackRecipe>> savedRecipesArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(freeIpaStackRecipeRepository).saveAll(savedRecipesArgumentCaptor.capture());
        assertThat(savedRecipesArgumentCaptor.getValue()).extracting(FreeIpaStackRecipe::getRecipe).contains("recipe3", "recipe4");
        verify(recipeUsageService, times(2)).sendAttachedUsageReport(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void testAttachRecipesIfOneRecipeIsAttached() {
        List<String> recipes = List.of("recipe3", "recipe4");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"),
                new FreeIpaStackRecipe(2L, "recipe2"),  new FreeIpaStackRecipe(3L, "recipe3"));
        String accid = "accid";
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", accid)).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        ArgumentCaptor<Collection> validationArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        freeIpaRecipeService.attachRecipes(accid, recipeAttachDetachRequest);
        verify(recipeCrnListProviderService).validateRequestedRecipesExistsByName(validationArgumentCaptor.capture());
        assertThat(validationArgumentCaptor.getValue()).containsExactlyInAnyOrder("recipe3", "recipe4");
        ArgumentCaptor<Set<FreeIpaStackRecipe>> savedRecipesArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(freeIpaStackRecipeRepository).saveAll(savedRecipesArgumentCaptor.capture());
        assertThat(savedRecipesArgumentCaptor.getValue()).extracting(FreeIpaStackRecipe::getRecipe).contains("recipe4");
        verify(recipeUsageService, times(1)).sendAttachedUsageReport(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void testDetachRecipes() {
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(2L, "recipe2"));
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", "accid")).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<String> recipes = List.of("recipe2");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        freeIpaRecipeService.detachRecipes("accid", recipeAttachDetachRequest);
        ArgumentCaptor<List<String>> deletedRecipesArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaStackRecipeRepository).deleteFreeIpaStackRecipeByStackIdAndRecipeIn(eq(1L), deletedRecipesArgumentCaptor.capture());
        assertThat(deletedRecipesArgumentCaptor.getValue()).contains("recipe2");
        verify(recipeUsageService, times(1)).sendDetachedUsageReport(anyString(), any(), any(), anyString(), any());
    }

    @Test
    void testDetachRecipesButThrowException() {
        List<FreeIpaStackRecipe> freeIpaStackRecipes = List.of(new FreeIpaStackRecipe(1L, "recipe1"), new FreeIpaStackRecipe(2L, "recipe2"));
        when(stackService.getResourceBasicViewByEnvironmentCrnAndAccountId("crn", "accid")).thenReturn(getBasicView(1L, "crn"));
        when(freeIpaStackRecipeRepository.findByStackId(1L)).thenReturn(freeIpaStackRecipes);
        List<String> recipes = List.of("recipe3");
        RecipeAttachDetachRequest recipeAttachDetachRequest = new RecipeAttachDetachRequest();
        recipeAttachDetachRequest.setRecipes(recipes);
        recipeAttachDetachRequest.setEnvironmentCrn("crn");
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> freeIpaRecipeService.detachRecipes("accid", recipeAttachDetachRequest));
        assertEquals("recipe3 recipe(s) are not attached to freeipa stack!", badRequestException.getMessage());
        verifyNoInteractions(recipeUsageService);
    }

    private ResourceBasicView getBasicView(long id, String crn) {
        return new ResourceBasicView() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getResourceCrn() {
                return crn;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getEnvironmentCrn() {
                return null;
            }
        };
    }
}
