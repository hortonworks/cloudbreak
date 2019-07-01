package com.sequenceiq.cloudbreak.service.recipe;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.dto.RecipeAccessDto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.RecipeViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractArchivistService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Service
public class RecipeService extends AbstractArchivistService<Recipe> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    @Inject
    private RecipeRepository recipeRepository;

    @Inject
    private RecipeViewRepository recipeViewRepository;

    @Inject
    private HostGroupService hostGroupService;

    public Recipe delete(RecipeAccessDto recipeAccessDto, Long workspaceId) {
        validateDto(recipeAccessDto);
        Recipe toDelete = get(recipeAccessDto, workspaceId);
        return super.delete(toDelete);
    }

    public Recipe get(RecipeAccessDto recipeAccessDto, Long workspaceId) {
        validateDto(recipeAccessDto);
        return isNotEmpty(recipeAccessDto.getName())
                ? super.getByNameForWorkspaceId(recipeAccessDto.getName(), workspaceId)
                : recipeRepository.findByCrnAndWorkspaceId(recipeAccessDto.getCrn(), workspaceId)
                        .orElseThrow(() -> new NotFoundException("No recipe found with crn: \"" + recipeAccessDto.getCrn() + "\""));
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
        recipe.setCrn(createCRN(accountId));
        recipe.setCreator(creator);
        return super.createForLoggedInUser(recipe, workspaceId);
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
    public WorkspaceResource resource() {
        return WorkspaceResource.RECIPE;
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
    }

    private void validateDto(RecipeAccessDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException("RecipeAccessDto should not be null"));
        if (dto.isNotValid()) {
            throw new BadRequestException("One and only one value of the crn and name should be filled!");
        }
    }

    private String createCRN(String accountId) {
        throwIfNull(accountId, IllegalArgumentException::new);
        return Crn.builder()
                .setService(Crn.Service.CLOUDBREAK)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.RECIPE)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

}
