package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_CONTAINER_ORCHESTRATOR;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.BootstrapApiContext;
import com.sequenceiq.cloudbreak.core.flow.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorTool;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;

@Component
public class ClusterBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Value("${cb.container.orchestrator:" + CB_CONTAINER_ORCHESTRATOR + "}")
    private ContainerOrchestratorTool containerOrchestratorTool;

    @javax.annotation.Resource
    private Map<ContainerOrchestratorTool, ContainerOrchestrator> containerOrchestrators;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private PollingService<BootstrapApiContext> bootstrapApiPollingService;

    @Autowired
    private BootstrapApiCheckerTask bootstrapApiCheckerTask;

    @Autowired
    private PollingService<ContainerOrchestratorClusterContext> clusterAvailabilityPollingService;

    @Autowired
    private ClusterAvailabilityCheckerTask clusterAvailabilityCheckerTask;

    public void bootstrapCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp()));
        }
        try {
            ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
            bootstrapApiPollingService.pollWithTimeout(
                    bootstrapApiCheckerTask,
                    new BootstrapApiContext(stack, gatewayInstance.getPublicIp(), containerOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            containerOrchestrator.bootstrap(gatewayInstance.getPublicIp(), nodes, stack.getConsulServers());
            clusterAvailabilityPollingService.pollWithTimeout(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapNewNodes(StackScalingContext stackScalingContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackScalingContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (stackScalingContext.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp()));
            }
        }
        try {
            ContainerOrchestrator containerOrchestrator = containerOrchestrators.get(containerOrchestratorTool);
            containerOrchestrator.bootstrapNewNodes(gatewayInstance.getPublicIp(), nodes);
            clusterAvailabilityPollingService.pollWithTimeout(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }
}
