package com.sequenceiq.cloudbreak.service.decorator;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.constraint.ConstraintTemplateService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
public class HostGroupDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private ConstraintTemplateService constraintTemplateService;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private ClusterService clusterService;

    public HostGroup decorate(HostGroup subject, HostGroupRequest hostGroupRequest, Long stackId, boolean postRequest, Long organizationId) {
        ConstraintJson constraintJson = hostGroupRequest.getConstraint();
        Set<Long> recipeIds = hostGroupRequest.getRecipeIds();
        Set<RecipeRequest> recipes = hostGroupRequest.getRecipes();
        Set<String> recipeNames = hostGroupRequest.getRecipeNames();

        LOGGER.debug("Decorating hostgroup on [{}] request.", postRequest ? "POST" : "PUT");
        Constraint constraint = conversionService.convert(constraintJson, Constraint.class);
        if (postRequest) {
            constraint = decorateConstraint(stackId, constraint, constraintJson.getInstanceGroupName(), constraintJson.getConstraintTemplateName());
            subject.setConstraint(constraint);
        } else {
            subject = getHostGroup(stackId, constraint, constraintJson, subject);
        }

        subject.getRecipes().clear();
        if (recipeIds != null) {
            prepareRecipesByIds(subject, recipeIds);
        }
        if (recipeNames != null && !recipeNames.isEmpty()) {
            prepareRecipesByName(subject, recipeNames, organizationId);
        }
        if (recipes != null && !recipes.isEmpty()) {
            prepareRecipesByRequests(subject, recipes, organizationId);
        }

        return subject;
    }

    private void prepareRecipesByName(HostGroup subject, Collection<String> recipeNames, Long organizationId) {
        Set<Recipe> recipes = recipeNames.stream()
                .map(name -> recipeService.getByNameForOrganizationId(name, organizationId))
                .collect(Collectors.toSet());
        subject.getRecipes().addAll(recipes);
    }

    private void prepareRecipesByRequests(HostGroup subject, Iterable<RecipeRequest> recipes, Long organizationId) {
        for (RecipeRequest recipe : recipes) {
            Recipe convertedRecipe = conversionService.convert(recipe, Recipe.class);
            convertedRecipe = recipeService.create(convertedRecipe, organizationId);
            subject.getRecipes().add(convertedRecipe);
        }
    }

    private void prepareRecipesByIds(HostGroup subject, Iterable<Long> recipeIds) {
        for (Long recipeId : recipeIds) {
            Recipe recipe = recipeService.getByIdFromAnyAvailableOrganization(recipeId);
            subject.getRecipes().add(recipe);
        }
    }

    private Constraint decorateConstraint(Long stackId, Constraint constraint, String instanceGroupName, String constraintTemplateName) {
        if (instanceGroupName != null) {
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, instanceGroupName);
            if (instanceGroup == null) {
                LOGGER.error("Instance group not found: {}", instanceGroupName);
                throw new BadRequestException(String.format("Instance group '%s' not found on stack.", instanceGroupName));
            }
            constraint.setInstanceGroup(instanceGroup);
        }
        if (constraintTemplateName != null) {
            IdentityUser user = authenticatedUserService.getCbUser();
            ConstraintTemplate constraintTemplate = constraintTemplateService.findByNameInAccount(constraintTemplateName, user.getAccount(), user.getUserId());
            if (constraintTemplate == null) {
                throw new BadRequestException(String.format("Couldn't find constraint template with name: %s", constraintTemplateName));
            }
            constraint.setConstraintTemplate(constraintTemplate);
        }
        return constraint;
    }

    private HostGroup getHostGroup(Long stackId, Constraint constraint, ConstraintJson constraintJson, HostGroup subject) {
        if (constraintJson == null) {
            throw new BadRequestException("The constraint field must be set in the reinstall request!");
        }
        HostGroup result = subject;
        String instanceGroupName = constraintJson.getInstanceGroupName();
        String constraintTemplateName = constraintJson.getConstraintTemplateName();
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stackId);
        Constraint decoratedConstraint = decorateConstraint(stackId, constraint, instanceGroupName, constraintTemplateName);
        if (!isEmpty(instanceGroupName)) {
            result = getHostGroupByInstanceGroupName(decoratedConstraint, subject, cluster, instanceGroupName);
        } else if (!isEmpty(constraintTemplateName)) {
            subject.setConstraint(constraintRepository.save(constraint));
        } else {
            throw new BadRequestException("The constraint field must contain the 'constraintTemplateName' or 'instanceGroupName' parameter!");
        }
        return result;
    }

    private HostGroup getHostGroupByInstanceGroupName(Constraint constraint, HostGroup subject, Cluster cluster, String instanceGroupName) {
        HostGroup result = subject;
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
        if (hostGroups.isEmpty()) {
            Stack stack = cluster.getStack();
            if (stack == null) {
                String msg = String.format("There is no stack associated to cluster (id:'%s', name: '%s')!", cluster.getId(), cluster.getName());
                throw new BadRequestException(msg);
            } else {
                subject.setConstraint(constraint);
            }
        } else {
            result = getDetailsFromExistingHostGroup(constraint, subject, instanceGroupName, hostGroups);
        }
        return result;
    }

    private HostGroup getDetailsFromExistingHostGroup(Constraint constraint, HostGroup subject, String instanceGroupName, Collection<HostGroup> hostGroups) {
        Optional<HostGroup> hostGroupOptional = hostGroups.stream().filter(input ->
                input.getConstraint().getInstanceGroup().getGroupName().equals(instanceGroupName)
        ).findFirst();
        if (hostGroupOptional.isPresent()) {
            HostGroup hostGroup = hostGroupOptional.get();
            Integer instanceGroupNodeCount = hostGroup.getConstraint().getInstanceGroup().getNodeCount();
            if (constraint.getHostCount() > instanceGroupNodeCount) {
                throw new BadRequestException(String.format("The 'hostCount' of host group '%s' constraint could not be more than '%s'!", subject.getName(),
                        instanceGroupNodeCount));
            }
            hostGroup.setName(subject.getName());
            return hostGroup;
        } else {
            throw new BadRequestException(String.format("Invalid 'instanceGroupName'! Could not find instance group with name: '%s'", instanceGroupName));
        }
    }
}
