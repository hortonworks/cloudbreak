package com.sequenceiq.cloudbreak.service.decorator;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.isGateWay;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class HostGroupDecorator implements Decorator<HostGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupDecorator.class);

    private enum DecorationData {
        STACK_ID,
        INSTANCEGROUP_NAME,
        RECEIPE_IDS,
        REQUEST_TYPE
    }

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RecipeRepository recipeRepository;

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
            if (isGateWay(instanceGroup.getInstanceGroupType())) {
                LOGGER.error("Cannot define hostgroup on gateway! stackId: {}, instancegroup name: {}", stackId, instanceGroupName);
                throw new BadRequestException(String.format("Cannot define hostgroup on gateway in stack '%s'", instanceGroupName, stackId));
            }
            subject.setInstanceGroup(instanceGroup);
            if (receipeIds != null) {
                for (Long recipeId : receipeIds) {
                    Recipe recipe = recipeRepository.findOne(recipeId);
                    subject.getRecipes().add(recipe);
                }
            }
        } else {
            subject = reloadHostGroup(stackId, instanceGroupName, subject.getName());
        }
        return subject;
    }

    private HostGroup reloadHostGroup(Long stackId, String instanceGroupName, String hostGroupName) {
        Stack stack = stackRepository.findById(stackId);
        HostGroup hostGroupsByInstanceGroupName = hostGroupRepository.findHostGroupsByInstanceGroupName(stack.getCluster().getId(), instanceGroupName);
        hostGroupsByInstanceGroupName.setName(hostGroupName);
        return hostGroupsByInstanceGroupName;
    }

}
