package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.MUNCHAUSEN;
import static com.sequenceiq.cloudbreak.service.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.service.PollingResult.TIMEOUT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
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
    private OrchestratorRepository orchestratorRepository;

    @Inject
    private PollingService<ContainerBootstrapApiContext> containerBootstrapApiPollingService;

    @Inject
    private PollingService<HostBootstrapApiContext> hostBootstrapApiPollingService;

    @Inject
    private ContainerBootstrapApiCheckerTask containerBootstrapApiCheckerTask;

    @Inject
    private HostBootstrapApiCheckerTask hostBootstrapApiCheckerTask;

    @Inject
    private PollingService<ContainerOrchestratorClusterContext> containerClusterAvailabilityPollingService;

    @Inject
    private PollingService<HostOrchestratorClusterContext> hostClusterAvailabilityPollingService;

    @Inject
    private ContainerClusterAvailabilityCheckerTask containerClusterAvailabilityCheckerTask;

    @Inject
    private HostClusterAvailabilityCheckerTask hostClusterAvailabilityCheckerTask;

    @Inject
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    public void bootstrapMachines(Long stackId) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        String stackOrchestratorType = stack.getOrchestrator().getType();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stackOrchestratorType);

        if (orchestratorType.hostOrchestrator()) {
            bootstrapOnHost(stack);
        } else if (OrchestratorConstants.MARATHON.equals(stackOrchestratorType)) {
            LOGGER.info("Skipping bootstrap of the macines because the stack's orchestrator type is '{}'.", stackOrchestratorType);
        } else if (orchestratorType.containerOrchestrator()) {
            bootstrapContainers(stack);
        } else {
            LOGGER.error("Orchestrator not found: {}", stackOrchestratorType);
            throw new CloudbreakException("HostOrchestrator not found: " + stackOrchestratorType);
        }
    }

    @SuppressWarnings("unchecked")
    public void bootstrapOnHost(Stack stack) throws CloudbreakException {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            Node node = new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper());
            node.setHostGroup(instanceMetaData.getInstanceGroupName());
            nodes.add(node);
        }
        try {
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithTimeoutSingleFailure(
                    hostBootstrapApiCheckerTask,
                    new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");
            hostOrchestrator.bootstrap(gatewayConfig, nodes, clusterDeletionBasedExitCriteriaModel(stack.getId(), null));

            PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    hostClusterAvailabilityCheckerTask,
                    new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.");
            Orchestrator orchestrator = stack.getOrchestrator();
            orchestrator.setApiEndpoint(gatewayInstance.getPublicIpWrapper() + ":" + stack.getGatewayPort());
            orchestrator.setType(hostOrchestrator.name());
            orchestratorRepository.save(orchestrator);
            if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapContainers(Stack stack) throws CloudbreakException {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper()));
        }
        try {
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance);
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(SWARM);
            PollingResult bootstrapApiPolling = containerBootstrapApiPollingService.pollWithTimeoutSingleFailure(
                    containerBootstrapApiCheckerTask,
                    new ContainerBootstrapApiContext(stack, gatewayConfig, containerOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");

            List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator.getMaxBootstrapNodes(), gatewayInstance.getPublicIpWrapper());
            containerOrchestrator.bootstrap(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap.get(0), stack.getConsulServers(),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
            if (nodeMap.size() > 1) {
                PollingResult gatewayAvailability = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                        containerClusterAvailabilityCheckerTask,
                        new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap.get(0)),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS);
                validatePollingResultForCancellation(gatewayAvailability, "Polling of gateway node availability was cancelled.");
                for (int i = 1; i < nodeMap.size(); i++) {
                    containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap.get(i),
                            clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
                    PollingResult agentAvailability = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                            containerClusterAvailabilityCheckerTask,
                            new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap.get(i)),
                            POLLING_INTERVAL,
                            MAX_POLLING_ATTEMPTS);
                    validatePollingResultForCancellation(agentAvailability, "Polling of agent nodes availability was cancelled.");
                }
            }
            PollingResult allNodesAvailabilityPolling = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    containerClusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.");
            // TODO: put it in orchestrator api
            Orchestrator orchestrator = new Orchestrator();
            orchestrator.setApiEndpoint(gatewayInstance.getPublicIpWrapper() + ":" + stack.getGatewayPort());
            orchestrator.setType("SWARM");
            orchestratorRepository.save(orchestrator);
            stack.setOrchestrator(orchestrator);
            stackRepository.save(stack);
            if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(null, containerOrchestrator, stack, gatewayConfig, nodes);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapNewNodes(Long stackId, Set<String> upscaleCandidateAddresses) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackId);

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (upscaleCandidateAddresses.contains(instanceMetaData.getPrivateIp())) {
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper()));
            }
        }
        try {
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance);
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType.hostOrchestrator()) {
                bootstrapNewNodesOnHost(stack, gatewayInstance, nodes, gatewayConfig);
            } else if (orchestratorType.containerOrchestrator()) {
                bootstrapNewNodesInContainer(stack, gatewayInstance, nodes, gatewayConfig);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private void bootstrapNewNodesOnHost(Stack stack, InstanceMetaData gatewayInstance, Set<Node> nodes, GatewayConfig gatewayConfig)
            throws CloudbreakException, CloudbreakOrchestratorException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, hostOrchestrator.getMaxBootstrapNodes(), gatewayInstance.getPublicIpWrapper());
        PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithTimeoutSingleFailure(
                hostBootstrapApiCheckerTask,
                new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");
        Set<InstanceMetaData> runningInstanceMetaData = stack.getRunningInstanceMetaData();
        for (Set<Node> nodeSet : nodeMap) {
            nodeSet.forEach(n -> n.setHostGroup(
                    runningInstanceMetaData.stream().filter(i -> i.getPrivateIp().equals(n.getPrivateIp())).findFirst().get().getInstanceGroupName()));
            hostOrchestrator.bootstrapNewNodes(gatewayConfig, nodeSet, clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
            PollingResult newNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    hostClusterAvailabilityCheckerTask,
                    new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodeSet),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(newNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
        }
        PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                hostClusterAvailabilityCheckerTask,
                new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
        if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
        }
    }

    private void bootstrapNewNodesInContainer(Stack stack, InstanceMetaData gatewayInstance, Set<Node> nodes, GatewayConfig gatewayConfig)
            throws CloudbreakException, CloudbreakOrchestratorException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(SWARM);
        List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator.getMaxBootstrapNodes(), gatewayInstance.getPublicIpWrapper());
        for (Set<Node> aNodeMap : nodeMap) {
            containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), aNodeMap,
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
            PollingResult newNodesAvailabilityPolling = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    containerClusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, aNodeMap),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(newNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
        }
        PollingResult pollingResult = containerClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                containerClusterAvailabilityCheckerTask,
                new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                POLLING_INTERVAL,
                MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(pollingResult, "Polling of new nodes availability was cancelled.");
        if (TIMEOUT.equals(pollingResult)) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(null, containerOrchestrator, stack, gatewayConfig, nodes);
        }
    }

    private List<Set<Node>> prepareBootstrapSegments(Set<Node> nodes, int maxBootstrapNodes, String gatewayIp) {
        List<Set<Node>> result = new ArrayList<>();
        Set<Node> newNodes = new HashSet<>();
        Node gatewayNode = getGateWayNode(nodes, gatewayIp);
        if (gatewayNode != null) {
            newNodes.add(gatewayNode);
        }
        for (Node node : nodes) {
            if (!gatewayIp.equals(node.getPublicIp())) {
                newNodes.add(node);
                if (newNodes.size() >= maxBootstrapNodes) {
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

    private void validatePollingResultForCancellation(PollingResult pollingResult, String cancelledMessage) {
        if (EXIT.equals(pollingResult)) {
            throw new CancellationException(cancelledMessage);
        }
    }

}
