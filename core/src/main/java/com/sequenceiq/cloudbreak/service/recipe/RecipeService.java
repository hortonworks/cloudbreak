package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static java.util.Collections.emptySet;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.usage.service.RecipeUsageService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class RecipeService extends AbstractArchivistService<Recipe> implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    private static final int MAX_RECIPE_SIZE_PER_WORKSPACE = 2500;

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private RecipeViewRepository recipeViewRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private RecipeUsageService recipeUsageService;

    public void sendClusterCreationUsageReport(Stack stack) {
        try {
            String stackCrn = stack.getResourceCrn();
            int recipeCount = (int) stack.getCluster().getHostGroups()
                    .stream()
                    .mapToLong(hg -> hg.getRecipes().size())
                    .sum();
            Map<String, Integer> hostGroupDetails = stack.getCluster().getHostGroups()
                    .stream()
                    .collect(Collectors.toMap(HostGroup::getName, hg -> CollectionUtils.emptyIfNull(hg.getRecipes()).size()));
            Map<RecipeType, AtomicInteger> typeDetails = Arrays.stream(RecipeType.values())
                    .collect(Collectors.toMap(rt -> rt, rt -> new AtomicInteger(0)));
            stack.getCluster().getHostGroups()
                    .stream()
                    .flatMap(hg -> hg.getRecipes().stream())
                    .forEach(recipe -> typeDetails.get(recipe.getRecipeType()).incrementAndGet());
            recipeUsageService.sendClusterCreationRecipeUsageReport(stackCrn, recipeCount,
                    Optional.ofNullable(JsonUtil.writeValueAsStringSilentSafe(typeDetails)),
                    Optional.ofNullable(JsonUtil.writeValueAsStringSilentSafe(hostGroupDetails)));
        } catch (Exception e) {
            LOGGER.error(String.format("We could not send usage report regarding recipes during cluster creation for stack %s, because: ",
                    stack.getResourceCrn()), e.getMessage());
        }
    }

    public Recipe delete(NameOrCrn recipeNameOrCrn, Long workspaceId) {
        Recipe toDelete = get(recipeNameOrCrn, workspaceId);
        Recipe deleted = super.delete(toDelete, this::prepareDeletion);
        ownerAssignmentService.notifyResourceDeleted(deleted.getResourceCrn());
        recipeUsageService.sendDeletedUsageReport(deleted.getName(), deleted.getResourceCrn(), deleted.getRecipeTypeString());
        return deleted;
    }

    @Override
    public Set<Recipe> delete(Set<Recipe> resources) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> {
            List<String> recipesUsedInFMS = freeipaClientService.recipes(accountId);
            return resources.stream().peek(recipe -> super.delete(recipe, r -> prepareDeletion(recipe, recipesUsedInFMS))).collect(Collectors.toSet());
        });
    }

    @Override
    public Set<Recipe> deleteMultipleByNameFromWorkspace(Set<String> names, Long workspaceId) {
        Set<Recipe> deletedRecipes = super.deleteMultipleByNameFromWorkspace(names, workspaceId);
        deletedRecipes.forEach(deleted -> ownerAssignmentService.notifyResourceDeleted(deleted.getResourceCrn()));
        deletedRecipes.forEach(deleted ->
                recipeUsageService.sendDeletedUsageReport(deleted.getName(), deleted.getResourceCrn(), deleted.getRecipeTypeString()));
        return deletedRecipes;
    }

    public Recipe get(NameOrCrn recipeNameOrCrn, Long workspaceId) {
        return recipeNameOrCrn.hasName()
                ? super.getByNameForWorkspaceId(recipeNameOrCrn.getName(), workspaceId)
                : recipeRepository.findByResourceCrnAndWorkspaceId(recipeNameOrCrn.getCrn(), workspaceId)
                .orElseThrow(() -> new NotFoundException("No recipe found with crn: \"" + recipeNameOrCrn + "\""));
    }

    public Set<Recipe> getRecipesByNamesForWorkspace(Workspace workspace, Set<String> recipeNames) {
        return getRecipesByNamesForWorkspace(workspace.getId(), recipeNames);
    }

    public Set<Recipe> getRecipesByNamesForWorkspace(Long workspaceId, Set<String> recipeNames) {
        if (recipeNames.isEmpty()) {
            return emptySet();
        }
        Set<Recipe> recipes = recipeRepository.findByNameInAndWorkspaceId(recipeNames, workspaceId);
        if (recipeNames.size() != recipes.size()) {
            throw new NotFoundException(String.format("Recipes '%s' not found.", collectMissingRecipeNames(recipes, recipeNames)));
        }
        return recipes;
    }

    public Recipe createForLoggedInUser(Recipe recipe, @Nonnull Long workspaceId, String accountId, String creator) {
        if (recipeViewRepository.findByNameAndWorkspaceId(recipe.getName(), workspaceId).isPresent()) {
            String message = String.format("%s already exists with name '%s'", recipe.getResourceName(), recipe.getName());
            throw new BadRequestException(message);
        }
        if (recipeRepository.countByAccountId(accountId) >= MAX_RECIPE_SIZE_PER_WORKSPACE) {
            throw new BadRequestException("Max recipe limit reached in account. Please remove unused recipes.");
        }
        String resourceCrn = createCRN(accountId);
        recipe.setResourceCrn(resourceCrn);
        recipe.setCreator(creator);
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(), resourceCrn);
        try {
            Recipe created = super.createForLoggedInUserInTransaction(recipe, workspaceId);
            recipeUsageService.sendCreatedUsageReport(created.getName(), resourceCrn, created.getRecipeTypeString());
            return created;
        } catch (RuntimeException e) {
            ownerAssignmentService.notifyResourceDeleted(resourceCrn);
            throw e;
        }
    }

    public Recipe createWithInternalUser(Recipe recipe, @Nonnull Long workspaceId, String accountId) {
        if (recipeViewRepository.findByNameAndWorkspaceId(recipe.getName(), workspaceId).isPresent()) {
            String message = String.format("%s already exists with name '%s'", recipe.getResourceName(), recipe.getName());
            throw new BadRequestException(message);
        }
        recipe.setResourceCrn(createCRN(accountId));
        try {
            return transactionService.required(() -> {
                Workspace workspace = getWorkspaceService().getByIdWithoutAuth(workspaceId);
                recipe.setWorkspace(workspace);
                recipe.setCreationType(CreationType.SERVICE);
                Recipe created = super.pureSave(recipe);
                recipeUsageService.sendCreatedUsageReport(created.getName(), created.getResourceCrn(), created.getRecipeTypeString());
                return created;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private String collectMissingRecipeNames(Set<Recipe> recipes, Collection<String> recipeNames) {
        Set<String> foundRecipes = recipes.stream().map(Recipe::getName).collect(Collectors.toSet());
        return recipeNames.stream().filter(r -> !foundRecipes.contains(r)).collect(Collectors.joining(","));
    }

    public Set<RecipeView> findAllViewByWorkspaceId(Long workspaceId) {
        return recipeViewRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public WorkspaceResourceRepository<Recipe, Long> repository() {
        return recipeRepository;
    }

    private void prepareDeletion(Recipe recipe, List<String> recipesUsedInFMS) {
        checkIfRecipeUsedByHostGroups(recipe);
        recipeUsedInFMS(recipe, recipesUsedInFMS);
    }

    @Override
    protected void prepareDeletion(Recipe recipe) {
        checkIfRecipeUsedByHostGroups(recipe);
        Crn resourceCrn = Crn.fromString(recipe.getResourceCrn());
        if (resourceCrn != null) {
            ThreadBasedUserCrnProvider.doAsInternalActor(() -> {
                List<String> recipesUsedInFMS = freeipaClientService.recipes(resourceCrn.getAccountId());
                recipeUsedInFMS(recipe, recipesUsedInFMS);
            });
        }
    }

    private void recipeUsedInFMS(Recipe recipe, List<String> recipesUsedInFMS) {
        if (!recipesUsedInFMS.isEmpty()) {
            if (recipesUsedInFMS.contains(recipe.getName())) {
                throw new BadRequestException(String.format("There are FreeIPA clusters associated with recipe '%s'. " +
                        "Please remove these before deleting the recipe.", recipe.getName()));
            }
        }
    }

    private void checkIfRecipeUsedByHostGroups(Recipe recipe) {
        if (recipe == null) {
            throw new NotFoundException("Recipe not found.");
        }
        LOGGER.debug("Check if recipe can be deleted. {} - {}", recipe.getId(), recipe.getName());
        Set<String> clustersWithRecipe = clusterService.findAllClusterNamesByRecipeId(recipe.getId());
        if (clustersWithRecipe.size() == 1) {
            String onlyCluster = clustersWithRecipe.iterator().next();
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses recipe '%s'. Please remove this "
                    + "cluster before deleting the recipe.", onlyCluster, recipe.getName()));
        } else if (clustersWithRecipe.size() > 1) {
            String clusterNames = clustersWithRecipe
                    .stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(
                    "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe. "
                            + "The following clusters are using this recipe: [%s].", recipe.getName(), clusterNames));
        }
    }

    @Override
    protected void prepareCreation(Recipe resource) {
        resource.setCreated(System.currentTimeMillis());
    }

    private String createCRN(String accountId) {
        throwIfNull(accountId, IllegalArgumentException::new);
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.RECIPE, accountId);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return recipeViewRepository.findResourceCrnByNameAndTenantId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("recipe", resourceName));
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return recipeViewRepository.findAllResourceCrnsByNamesAndTenantId(resourceNames, ThreadBasedUserCrnProvider.getAccountId());
    }

    public Set<String> getResourceNamesByNamesAndWorkspaceId(List<String> resourceNames, long workspaceId) {
        return recipeViewRepository.findAllNamesByNamesAndWorkspaceId(resourceNames, workspaceId);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.RECIPE;
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        recipeViewRepository.findResourceNamesByCrnAndTenantId(crns, ThreadBasedUserCrnProvider.getAccountId()).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.RECIPE);
    }

    public Set<RecipeView> findAllViewById(List<Long> ids) {
        return Sets.newLinkedHashSet(recipeViewRepository.findAllByIdNotArchived(ids));
    }

    public List<ResourceWithId> findAsAuthorizationResourcesInWorkspace(Long workspaceId) {
        return recipeViewRepository.findAsAuthorizationResourcesInWorkspace(workspaceId);
    }
}
