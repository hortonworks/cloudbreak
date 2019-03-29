package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
public class HostGroupDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    @Inject
    private RecipeService recipeService;

    public HostGroup decorate(HostGroup subject, HostGroupV4Request hostGroupV4Request, Stack stack, boolean postRequest) {
        Set<String> recipeNames = hostGroupV4Request.getRecipeNames();
        LOGGER.debug("Decorating hostgroup on [{}] request.", postRequest ? "POST" : "PUT");
        subject.getRecipes().clear();
        if (recipeNames != null && !recipeNames.isEmpty()) {
            prepareRecipesByName(subject, stack.getWorkspace(), recipeNames);
        }
        return subject;
    }

    private void prepareRecipesByName(HostGroup subject, Workspace workspace, Set<String> recipeNames) {
        Set<Recipe> recipes = recipeService.getRecipesByNamesForWorkspace(workspace, recipeNames);
        subject.getRecipes().addAll(recipes);
    }

}
