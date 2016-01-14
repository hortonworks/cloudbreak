package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.api.model.InstanceGroupType.isGateway;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
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
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConversionService conversionService;

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
        if (postRequest) {
            Constraint constraint = conversionService.convert(constraintJson, Constraint.class);
            constraint = decorateConstraint(stackId, user, constraint, constraintJson.getInstanceGroupName(), constraintJson.getConstraintTemplateName());
            subject.setConstraint(constraint);
        } else {
            //TODO: find hostgroups for cluster, delete them and recreate them
//            subject = reloadHostGroup(stackId, instanceGroupName, subject.getName());
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
            if (isGateway(instanceGroup.getInstanceGroupType())) {
                LOGGER.error("Cannot define hostgroup on gateway! Instance group: {}", instanceGroupName);
                throw new BadRequestException(String.format("Cannot define hostgroup on gateway! Instance group: '%s'", instanceGroupName));
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

//    private HostGroup reloadHostGroup(Long stackId, String instanceGroupName, String hostGroupName) {
//        Stack stack = stackService.get(stackId);
//        HostGroup hostGroupsByInstanceGroupName = hostGroupService.getByClusterIdAndInstanceGroupName(stack.getCluster().getId(), instanceGroupName);
//        hostGroupsByInstanceGroupName.setName(hostGroupName);
//        return hostGroupsByInstanceGroupName;
//    }

}
