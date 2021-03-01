package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.FlowTriggerCondition;

@Component
    public class StackLoadBalancerUpdateTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackLoadBalancerUpdateTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        Stack stack = stackService.getByIdWithTransaction(stackId);

        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

        Set<InstanceGroup> instanceGroups = instanceGroupService.findByStackId(stack.getId());
        stack.setInstanceGroups(instanceGroups);
        if (!loadBalancerConfigService.isLoadBalancerCreationConfigured(stack, environment)) {
            LOGGER.debug("Load balancer update could not be configured because load balancers are not enabled for the stack. " +
                "Check that correct entitlements are enabled and the environment has valid network settings. Ending flow.");
            return false;
        } else {
            return true;
        }
    }
}
