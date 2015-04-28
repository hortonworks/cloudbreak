package com.sequenceiq.cloudbreak.core.flow;

import java.util.Map;

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

    @Value("${cb.container.orchestrator:SWARM}")
    private ContainerOrchestratorTool containerOrchestratorTool;

    @javax.annotation.Resource
    private Map<ContainerOrchestratorTool, ContainerOrchestrator> containerOrchestrators;

    @Autowired
    private StackRepository stackRepository;

    public FlowContext setup(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack);

        ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
        ContainerOrchestratorClient client = containerOrchestrator.bootstrap(provisioningContext.getStackId());
        containerOrchestrator.startRegistrator(client, provisioningContext.getStackId());
        containerOrchestrator.startAmbariServer(client, provisioningContext.getStackId());
        containerOrchestrator.startAmbariAgents(client, provisioningContext.getStackId());
        containerOrchestrator.startConsulWatches(client, provisioningContext.getStackId());

        return new ProvisioningContext.Builder()
                .setAmbariIp(provisioningContext.getAmbariIp())
                .setDefaultParams(stack.getId(), stack.cloudPlatform())
                .build();
    }

    public void setupNewNode(ClusterScalingContext clusterScalingContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(clusterScalingContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
        containerOrchestrator.preSetupNewNode(clusterScalingContext.getStackId(), gateway, clusterScalingContext.getUpscaleIds());
        containerOrchestrator.newHostgroupNodesSetup(clusterScalingContext.getStackId(), clusterScalingContext.getUpscaleIds(),
                clusterScalingContext.getHostGroupAdjustment().getHostGroup());
    }

}
