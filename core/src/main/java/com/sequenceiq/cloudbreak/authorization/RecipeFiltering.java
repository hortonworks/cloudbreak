package com.sequenceiq.cloudbreak.authorization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class RecipeFiltering extends AbstractAuthorizationFiltering<Set<RecipeView>> {

    @Inject
    private RecipeService recipeService;

    @Inject
    private WorkspaceService workspaceService;

    public Set<RecipeView> filterRecipes(AuthorizationResourceAction action) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of());
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return recipeService.findAsAuthorizationResourcesInWorkspace(workspaceService.getForCurrentUser().getId());
    }

    @Override
    public Set<RecipeView> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return Sets.newLinkedHashSet(recipeService.findAllViewById(authorizedResourceIds));
    }

    @Override
    public Set<RecipeView> getAll(Map<String, Object> args) {
        return recipeService.findAllViewByWorkspaceId(workspaceService.getForCurrentUser().getId());
    }
}
