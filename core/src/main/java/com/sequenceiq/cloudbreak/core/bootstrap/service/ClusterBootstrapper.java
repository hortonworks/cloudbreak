package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.service.PollingResult.TIMEOUT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;

@Component
public class ClusterBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    private static final int POLLING_INTERVAL = 5000;

    private static final int MAX_POLLING_ATTEMPTS = 500;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private PollingService<BootstrapApiContext> bootstrapApiPollingService;

    @Inject
    private BootstrapApiCheckerTask bootstrapApiCheckerTask;

    @Inject
    private PollingService<ContainerOrchestratorClusterContext> clusterAvailabilityPollingService;

    @Inject
    private ClusterAvailabilityCheckerTask clusterAvailabilityCheckerTask;

    @Inject
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    public void bootstrapCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp()));
        }
        try {
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
            bootstrapApiPollingService.pollWithTimeout(
                    bootstrapApiCheckerTask,
                    new BootstrapApiContext(stack, gatewayInstance.getPublicIp(), containerOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator, gatewayInstance.getPublicIp());
            containerOrchestrator.bootstrap(gatewayInstance.getPublicIp(), nodeMap.get(0), stack.getConsulServers());
            if (nodeMap.size() > 1) {
                clusterAvailabilityPollingService.pollWithTimeout(clusterAvailabilityCheckerTask,
                        new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodeMap.get(0)),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS);
                for (int i = 1; i < nodeMap.size(); i++) {
                    containerOrchestrator.bootstrapNewNodes(gatewayInstance.getPublicIp(), nodeMap.get(i));
                    clusterAvailabilityPollingService.pollWithTimeout(clusterAvailabilityCheckerTask,
                            new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodeMap.get(i)),
                            POLLING_INTERVAL,
                            MAX_POLLING_ATTEMPTS);
                }
            }
            PollingResult pollingResult = clusterAvailabilityPollingService.pollWithTimeout(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            if (TIMEOUT.equals(pollingResult)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(containerOrchestrator, stack, nodes);
            }
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
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
            List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator, gatewayInstance.getPublicIp());
            for (int i = 0; i < nodeMap.size(); i++) {
                containerOrchestrator.bootstrapNewNodes(gatewayInstance.getPublicIp(), nodeMap.get(i));
                clusterAvailabilityPollingService.pollWithTimeout(clusterAvailabilityCheckerTask,
                        new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodeMap.get(i)),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS);
            }
            PollingResult pollingResult = clusterAvailabilityPollingService.pollWithTimeout(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayInstance.getPublicIp(), nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            if (TIMEOUT.equals(pollingResult)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(containerOrchestrator, stack, nodes);
            }
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private List<Set<Node>> prepareBootstrapSegments(Set<Node> nodes, ContainerOrchestrator containerOrchestrator, String gatewayIp) {
        List<Set<Node>> result = new ArrayList<>();
        Set<Node> newNodes = new HashSet<>();
        Node gatewayNode = getGateWayNode(nodes, gatewayIp);
        if (gatewayNode != null) {
            newNodes.add(gatewayNode);
        }
        for (Node node : nodes) {
            if (!gatewayIp.equals(node.getPublicIp())) {
                newNodes.add(node);
                if (newNodes.size() >= containerOrchestrator.getMaxBootstrapNodes()) {
                    result.add(newNodes);
                    newNodes = new HashSet<>();
                }
            }
        }
        if (!newNodes.isEmpty()) {
            result.add(newNodes);
        }
        return result;
    }

    private Node getGateWayNode(Set<Node> nodes, String gatewayIp) {
        for (Node node : nodes) {
            if (gatewayIp.equals(node.getPublicIp())) {
                return node;
            }
        }
        return null;
    }

}
