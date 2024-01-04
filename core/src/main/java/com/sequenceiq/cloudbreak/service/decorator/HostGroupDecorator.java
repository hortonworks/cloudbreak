package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class HostGroupDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    @Inject
    private RecipeService recipeService;

    public HostGroup decorate(HostGroup subject, HostGroupV4Request hostGroupV4Request, StackView stack) {
        Set<String> recipeNames = hostGroupV4Request.getRecipeNames();
        LOGGER.debug("Decorating hostgroup {} on request.", subject.getName());
        subject.getRecipes().clear();
        if (recipeNames != null && !recipeNames.isEmpty()) {
            prepareRecipesByName(subject, stack.getWorkspaceId(), recipeNames);
        }
        return subject;
    }

    private void prepareRecipesByName(HostGroup subject, Long workspaceId, Set<String> recipeNames) {
        Set<Recipe> recipes = recipeService.getRecipesByNamesForWorkspace(workspaceId, recipeNames);
        subject.getRecipes().addAll(recipes);
    }

}
