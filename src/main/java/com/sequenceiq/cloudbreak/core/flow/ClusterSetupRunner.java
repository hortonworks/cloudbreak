package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.HOSTGROUP;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class ClusterSetupRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSetupRunner.class);

    @Value("${cb.cluster.setup.tool:SWARM}")
    private ClusterSetupTool clusterSetupTool;

    @javax.annotation.Resource
    private Map<ClusterSetupTool, ClusterSetupService> clusterSetupServices;

    @Autowired
    private StackRepository stackRepository;

    public FlowContext setup(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceGroup> hostGroups = stack.getInstanceGroupsByType(HOSTGROUP);
        InstanceGroup gateway = stack.getInstanceGroupsByType(GATEWAY).iterator().next();

        ClusterSetupService clusterSetupService = clusterSetupServices.get(clusterSetupTool);

        clusterSetupService.preSetup(provisioningContext.getStackId(), gateway, hostGroups);
        clusterSetupService.gatewaySetup(provisioningContext.getStackId(), gateway);
        clusterSetupService.hostgroupsSetup(provisioningContext.getStackId(), hostGroups);
        return clusterSetupService.postSetup(provisioningContext.getStackId());
    }

    public void setupNewNode(ClusterScalingContext clusterScalingContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(clusterScalingContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        InstanceGroup gateway = stack.getInstanceGroupsByType(GATEWAY).iterator().next();
        ClusterSetupService clusterSetupService = clusterSetupServices.get(clusterSetupTool);
        clusterSetupService.preSetupNewNode(clusterScalingContext.getStackId(), gateway, clusterScalingContext.getUpscaleIds());
        clusterSetupService.newHostgroupNodesSetup(clusterScalingContext.getStackId(), clusterScalingContext.getUpscaleIds(),
                clusterScalingContext.getHostGroupAdjustment().getHostGroup());
    }

}
