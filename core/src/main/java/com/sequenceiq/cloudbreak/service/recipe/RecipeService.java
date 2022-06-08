package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.CreationType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class RecipeService extends AbstractArchivistService<Recipe> implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private RecipeViewRepository recipeViewRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public Recipe delete(NameOrCrn recipeNameOrCrn, Long workspaceId) {
        Recipe toDelete = get(recipeNameOrCrn, workspaceId);
        Recipe deleted = super.delete(toDelete);
        ownerAssignmentService.notifyResourceDeleted(deleted.getResourceCrn());
        return deleted;
    }

    @Override
    public Set<Recipe> deleteMultipleByNameFromWorkspace(Set<String> names, Long workspaceId) {
        Set<Recipe> deletedRecipes = super.deleteMultipleByNameFromWorkspace(names, workspaceId);
        deletedRecipes.stream().forEach(deleted -> ownerAssignmentService.notifyResourceDeleted(deleted.getResourceCrn()));
        return deletedRecipes;
    }

    public Recipe get(NameOrCrn recipeNameOrCrn, Long workspaceId) {
        return recipeNameOrCrn.hasName()
                ? super.getByNameForWorkspaceId(recipeNameOrCrn.getName(), workspaceId)
                : recipeRepository.findByResourceCrnAndWorkspaceId(recipeNameOrCrn.getCrn(), workspaceId)
                .orElseThrow(() -> new NotFoundException("No recipe found with crn: \"" + recipeNameOrCrn + "\""));
    }

    public Set<Recipe> getRecipesByNamesForWorkspace(Workspace workspace, Set<String> recipeNames) {
        if (recipeNames.isEmpty()) {
            return emptySet();
        }
        Set<Recipe> recipes = recipeRepository.findByNameInAndWorkspaceId(recipeNames, workspace.getId());
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
        recipe.setResourceCrn(createCRN(accountId));
        recipe.setCreator(creator);
        try {
            return transactionService.required(() -> {
                Recipe created = super.createForLoggedInUser(recipe, workspaceId);
                ownerAssignmentService.assignResourceOwnerRoleIfEntitled(ThreadBasedUserCrnProvider.getUserCrn(), created.getResourceCrn(),
                        ThreadBasedUserCrnProvider.getAccountId());
                return created;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
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
                return super.pureSave(recipe);
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

    @Override
    protected void prepareDeletion(Recipe recipe) {
        if (recipe == null) {
            throw new NotFoundException("Recipe not found.");
        }
        LOGGER.debug("Check if recipe can be deleted. {} - {}", recipe.getId(), recipe.getName());
        List<HostGroup> hostGroupsWithRecipe = new ArrayList<>(hostGroupService.findAllHostGroupsByRecipe(recipe.getId()));
        if (!hostGroupsWithRecipe.isEmpty()) {
            if (hostGroupsWithRecipe.size() > 1) {
                String clusters = hostGroupsWithRecipe
                        .stream()
                        .map(hostGroup -> hostGroup.getCluster().getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe. "
                                + "The following clusters are using this recipe: [%s]", recipe.getId(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses recipe '%s'. Please remove this "
                    + "cluster before deleting the recipe", hostGroupsWithRecipe.get(0).getCluster().getName(), recipe.getName()));
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
