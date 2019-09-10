package com.sequenceiq.cloudbreak.service.decorator;


import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
public class HostGroupDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private ClusterService clusterService;

    public HostGroup decorate(HostGroup subject, HostGroupRequest hostGroupRequest, Stack stack, boolean postRequest, Workspace workspace, User user) {
        Set<Long> recipeIds = hostGroupRequest.getRecipeIds();
        Set<RecipeRequest> recipes = hostGroupRequest.getRecipes();
        Set<String> recipeNames = hostGroupRequest.getRecipeNames();
        LOGGER.debug("Decorating hostgroup on [{}] request.", postRequest ? "POST" : "PUT");
        if (postRequest) {
            subject = decorateHostGroupWithInstanceGroup(stack, subject, hostGroupRequest.getInstanceGroupName());
        } else {
            subject = getHostGroup(stack, hostGroupRequest.getInstanceGroupName(), subject);
        }

        subject.getRecipes().clear();
        if (recipeIds != null) {
            prepareRecipesByIds(subject, recipeIds);
        }
        if (recipeNames != null && !recipeNames.isEmpty()) {
            prepareRecipesByName(subject, stack.getWorkspace(), recipeNames);
        }
        if (recipes != null && !recipes.isEmpty()) {
            prepareRecipesByRequests(subject, recipes, workspace, user);
        }

        return subject;
    }

    private void prepareRecipesByName(HostGroup subject, Workspace workspace, Collection<String> recipeNames) {
        Set<Recipe> recipes = recipeService.getRecipesByNamesForWorkspace(workspace, recipeNames);
        subject.getRecipes().addAll(recipes);
    }

    private void prepareRecipesByRequests(HostGroup subject, Iterable<RecipeRequest> recipes, Workspace workspace, User user) {
        for (RecipeRequest recipe : recipes) {
            Recipe convertedRecipe = conversionService.convert(recipe, Recipe.class);
            convertedRecipe = recipeService.create(convertedRecipe, workspace, user);
            subject.getRecipes().add(convertedRecipe);
        }
    }

    private void prepareRecipesByIds(HostGroup subject, Iterable<Long> recipeIds) {
        for (Long recipeId : recipeIds) {
            Recipe recipe = recipeService.get(recipeId);
            subject.getRecipes().add(recipe);
        }
    }

    private HostGroup decorateHostGroupWithInstanceGroup(Stack stack, HostGroup hostGroup, String instanceGroupName) {
        if (StringUtils.isNotBlank(instanceGroupName)) {
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stack.getId(), instanceGroupName);
            if (instanceGroup == null) {
                LOGGER.error("Instance group not found: {}", instanceGroupName);
                throw new BadRequestException(String.format("Instance group '%s' not found on stack.", instanceGroupName));
            }
            hostGroup.setInstanceGroup(instanceGroup);
        }
        return hostGroup;
    }

    private HostGroup getHostGroup(Stack stack, String instanceGroupName, HostGroup subject) {
        if (StringUtils.isBlank(instanceGroupName)) {
            throw new BadRequestException("The instanceGroupName field must be set in the reinstall request!");
        }
        HostGroup result;
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId());
        result = decorateHostGroupWithInstanceGroup(stack, subject, instanceGroupName);
        result = getHostGroupByInstanceGroupName(result, cluster, instanceGroupName);
        return result;
    }

    private HostGroup getHostGroupByInstanceGroupName(HostGroup subject, Cluster cluster, String instanceGroupName) {
        HostGroup result = subject;
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
        if (hostGroups.isEmpty()) {
            if (cluster.getStack() == null) {
                String msg = String.format("There is no stack associated to cluster (id:'%s', name: '%s')!", cluster.getId(), cluster.getName());
                throw new BadRequestException(msg);
            }
        } else {
            result = getDetailsFromExistingHostGroup(subject, instanceGroupName, hostGroups);
        }
        return result;
    }

    private HostGroup getDetailsFromExistingHostGroup(HostGroup subject, String instanceGroupName, Collection<HostGroup> hostGroups) {
        Optional<HostGroup> hostGroupOptional = hostGroups.stream().filter(input ->
                input.getInstanceGroup().getGroupName().equals(instanceGroupName)
        ).findFirst();
        if (hostGroupOptional.isPresent()) {
            HostGroup hostGroup = hostGroupOptional.get();
            hostGroup.setName(subject.getName());
            return hostGroup;
        } else {
            throw new BadRequestException(String.format("Invalid 'instanceGroupName'! Could not find instance group with name: '%s'", instanceGroupName));
        }
    }
}
