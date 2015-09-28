package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.common.type.InstanceGroupType.isGateway;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class HostGroupDecorator implements Decorator<HostGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    private enum DecorationData {
        STACK_ID,
        INSTANCEGROUP_NAME,
        RECEIPE_IDS,
        REQUEST_TYPE
    }

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private RecipeService recipeService;

    @Override
    public HostGroup decorate(HostGroup subject, Object... data) {
        if (null == data || data.length != DecorationData.values().length) {
            throw new IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.getName());
        }
        Long stackId = (Long) data[DecorationData.STACK_ID.ordinal()];
        String instanceGroupName = (String) data[DecorationData.INSTANCEGROUP_NAME.ordinal()];
        Set<Long> receipeIds = (Set<Long>) data[DecorationData.RECEIPE_IDS.ordinal()];
        boolean postRequest = (boolean) data[DecorationData.REQUEST_TYPE.ordinal()];

        LOGGER.debug("Decorating hostgroup on [{}] request.", postRequest ? "POST" : "PUT");
        if (postRequest) {
            InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, instanceGroupName);
            if (instanceGroup == null) {
                LOGGER.error("No instancegroup found! stackId: {}, instancegroup name: {}", stackId, instanceGroupName);
                throw new BadRequestException(String.format("Cannot find instance group named '%s' in stack '%s'", instanceGroupName, stackId));
            }
            if (isGateway(instanceGroup.getInstanceGroupType())) {
                LOGGER.error("Cannot define hostgroup on gateway! stackId: {}, instancegroup name: {}", stackId, instanceGroupName);
                throw new BadRequestException(String.format("Cannot define hostgroup on gateway in stack '%s'", instanceGroupName, stackId));
            }
            subject.setInstanceGroup(instanceGroup);
        } else {
            subject = reloadHostGroup(stackId, instanceGroupName, subject.getName());
        }

        subject.getRecipes().clear();
        if (receipeIds != null) {
            for (Long recipeId : receipeIds) {
                Recipe recipe = recipeService.get(recipeId);
                subject.getRecipes().add(recipe);
            }
        }

        return subject;
    }

    private HostGroup reloadHostGroup(Long stackId, String instanceGroupName, String hostGroupName) {
        Stack stack = stackService.get(stackId);
        HostGroup hostGroupsByInstanceGroupName = hostGroupService.getByClusterIdAndInstanceGroupName(stack.getCluster().getId(), instanceGroupName);
        hostGroupsByInstanceGroupName.setName(hostGroupName);
        return hostGroupsByInstanceGroupName;
    }

}
