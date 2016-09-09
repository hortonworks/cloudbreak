package com.sequenceiq.cloudbreak.service.decorator;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class HostGroupDecorator implements Decorator<HostGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    private enum DecorationData {
        STACK_ID,
        USER,
        CONSTRAINT,
        RECIPE_IDS,
        REQUEST_TYPE
    }

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private ConstraintTemplateRepository constraintTemplateRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private ClusterService clusterService;

    @Override
    public HostGroup decorate(HostGroup subject, Object... data) {
        if (null == data || data.length != DecorationData.values().length) {
            throw new IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.getName());
        }
        Long stackId = (Long) data[DecorationData.STACK_ID.ordinal()];
        CbUser user = (CbUser) data[DecorationData.USER.ordinal()];
        ConstraintJson constraintJson = (ConstraintJson) data[DecorationData.CONSTRAINT.ordinal()];
        Set<Long> recipeIds = (Set<Long>) data[DecorationData.RECIPE_IDS.ordinal()];
        boolean postRequest = (boolean) data[DecorationData.REQUEST_TYPE.ordinal()];

        LOGGER.debug("Decorating hostgroup on [{}] request.", postRequest ? "POST" : "PUT");
        Constraint constraint = conversionService.convert(constraintJson, Constraint.class);
        if (postRequest) {
            constraint = decorateConstraint(stackId, user, constraint, constraintJson.getInstanceGroupName(), constraintJson.getConstraintTemplateName());
            subject.setConstraint(constraint);
        } else {
            subject = getHostGroup(stackId, constraint, constraintJson, subject, user);
        }

        subject.getRecipes().clear();
        if (recipeIds != null) {
            for (Long recipeId : recipeIds) {
                Recipe recipe = recipeService.get(recipeId);
                subject.getRecipes().add(recipe);
            }
        }

        return subject;
    }

    private Constraint decorateConstraint(Long stackId, CbUser user, Constraint constraint, String instanceGroupName, String constraintTemplateName) {
        if (instanceGroupName != null) {
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, instanceGroupName);
            if (instanceGroup == null) {
                LOGGER.error("Instance group not found: {}", instanceGroupName);
                throw new BadRequestException(String.format("Instance group '%s' not found on stack.", instanceGroupName));
            }
            constraint.setInstanceGroup(instanceGroup);
        }
        if (constraintTemplateName != null) {
            ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByNameInAccount(constraintTemplateName,
                    user.getAccount(), user.getUserId());
            if (constraintTemplate == null) {
                throw new BadRequestException(String.format("Couldn't find constraint template with name: %s", constraintTemplateName));
            }
            constraint.setConstraintTemplate(constraintTemplate);
        }
        return constraint;
    }

    private HostGroup getHostGroup(Long stackId, Constraint constraint, ConstraintJson constraintJson, HostGroup subject, CbUser user) {
        if (constraintJson == null) {
            throw new BadRequestException("The constraint field must be set in the reinstall request!");
        }
        HostGroup result = subject;
        final String instanceGroupName = constraintJson.getInstanceGroupName();
        final String constraintTemplateName = constraintJson.getConstraintTemplateName();
        Cluster cluster = clusterService.retrieveClusterByStackId(stackId);
        Constraint decoratedConstraint = decorateConstraint(stackId, user, constraint, instanceGroupName, constraintTemplateName);
        if (!isEmpty(instanceGroupName)) {
            result = getHostGroupByInstanceGroupName(decoratedConstraint, subject, cluster, instanceGroupName);
        } else if (!isEmpty(constraintTemplateName)) {
            subject.setConstraint(constraintRepository.save(constraint));
        } else {
            throw new BadRequestException("The constraint field must contain the 'constraintTemplateName' or 'instanceGroupName' parameter!");
        }
        return result;
    }

    private HostGroup getHostGroupByInstanceGroupName(Constraint constraint, HostGroup subject, Cluster cluster, final String instanceGroupName) {
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

    private HostGroup getDetailsFromExistingHostGroup(Constraint constraint, HostGroup subject, final String instanceGroupName, Set<HostGroup> hostGroups) {
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
            hostGroup.getConstraint().setHostCount(constraint.getHostCount());
            hostGroup.setName(subject.getName());
            return hostGroup;
        } else {
            throw new BadRequestException(String.format("Invalid 'instanceGroupName'! Could not find instance group with name: '%s'", instanceGroupName));
        }
    }
}
