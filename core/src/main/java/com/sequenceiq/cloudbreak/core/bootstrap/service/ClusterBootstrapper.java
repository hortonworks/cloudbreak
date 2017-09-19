package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.service.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.service.PollingResult.TIMEOUT;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
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
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ClusterBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    private static final int POLL_INTERVAL = 5000;

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

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    public void bootstrapMachines(Long stackId) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        String stackOrchestratorType = stack.getOrchestrator().getType();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stackOrchestratorType);

        if (orchestratorType.hostOrchestrator()) {
            bootstrapOnHost(stack);
        } else if (orchestratorType.containerOrchestrator()) {
            LOGGER.info("Skipping bootstrap of the machines because the stack's orchestrator type is '{}'.", stackOrchestratorType);
        } else {
            LOGGER.error("Orchestrator not found: {}", stackOrchestratorType);
            throw new CloudbreakException("HostOrchestrator not found: " + stackOrchestratorType);
        }
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @SuppressWarnings("unchecked")
    public void bootstrapOnHost(Stack stack) throws CloudbreakException {
        Set<Node> nodes = new HashSet<>();
        String domain = hostDiscoveryService.determineDomain(stack.getCustomDomain(), stack.getName(), stack.isClusterNameAsSubdomain());
        for (InstanceMetaData im : stack.getRunningInstanceMetaData()) {
            if (im.getPrivateIp() == null && im.getPublicIpWrapper() == null) {
                LOGGER.warn("Skipping instance metadata because the public ip and private ips are null '{}'.", im);
            } else {
                String generatedHostName = hostDiscoveryService.generateHostname(stack.getCustomHostname(), im.getInstanceGroupName(),
                        im.getPrivateId(), stack.isHostgroupNameAsHostname());
                nodes.add(new Node(im.getPrivateIp(), im.getPublicIpWrapper(), generatedHostName, domain, im.getInstanceGroupName()));
            }
        }
        try {
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            List<GatewayConfig> allGatewayConfig = new ArrayList<>();
            Boolean enableKnox = stack.getCluster().getGateway().getEnableGateway();
            for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
                GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
                allGatewayConfig.add(gatewayConfig);
                PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithTimeoutSingleFailure(
                        hostBootstrapApiCheckerTask, new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
                validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");
            }

            ClusterComponent saltComponent = clusterComponentProvider.getComponent(stack.getCluster().getId(), ComponentType.SALT_STATE);
            if (saltComponent == null) {
                byte[] stateConfigZip = hostOrchestrator.getStateConfigZip();
                saltComponent = new ClusterComponent(ComponentType.SALT_STATE,
                        new Json(singletonMap(ComponentType.SALT_STATE.name(), Base64.encodeBase64String(stateConfigZip))), stack.getCluster());
                clusterComponentProvider.store(saltComponent);
            }
            hostOrchestrator.bootstrap(allGatewayConfig, nodes, clusterDeletionBasedModel(stack.getId(), null));

            InstanceMetaData primaryGateway = stack.getPrimaryGatewayInstance();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, primaryGateway, enableKnox);
            String gatewayIp = gatewayConfigService.getGatewayIp(stack, primaryGateway);
            PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    hostClusterAvailabilityCheckerTask, new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                    POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.");

            Orchestrator orchestrator = stack.getOrchestrator();
            orchestrator.setApiEndpoint(gatewayIp + ':' + stack.getGatewayPort());
            orchestrator.setType(hostOrchestrator.name());
            orchestratorRepository.save(orchestrator);
            if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapNewNodes(Long stackId, Set<String> upscaleCandidateAddresses, Set<String> recoveryHostNames) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Set<Node> nodes = new HashSet<>();
        Set<Node> allNodes = new HashSet<>();
        boolean recoveredNodes = Integer.valueOf(recoveryHostNames.size()).equals(upscaleCandidateAddresses.size());
        Set<InstanceMetaData> metaDataSet = stack.getRunningInstanceMetaData().stream()
                .filter(im -> im.getPrivateIp() != null && im.getPublicIpWrapper() != null).collect(Collectors.toSet());
        String clusterDomain = metaDataSet.stream().filter(im -> isNoneBlank(im.getDiscoveryFQDN())).findAny().get().getDomain();
        for (InstanceMetaData im : metaDataSet) {
            Node node = createNode(stack.getCustomHostname(), im, clusterDomain, stack.isHostgroupNameAsHostname());
            if (upscaleCandidateAddresses.contains(im.getPrivateIp())) {
                // use the hostname of the node we're recovering instead of generating a new one
                // but only when we would have generated a hostname, otherwise use the cloud provider's default mechanism
                if (recoveredNodes && isNoneBlank(node.getHostname())) {
                    Iterator<String> iterator = recoveryHostNames.iterator();
                    node.setHostname(iterator.next().split("\\.")[0]);
                    iterator.remove();
                    LOGGER.info("Set the hostname to {} for address: {}", node.getHostname(), im.getPrivateIp());
                }
                nodes.add(node);
            }
            allNodes.add(node);
        }
        try {
            String stackOrchestratorType = stack.getOrchestrator().getType();
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType.hostOrchestrator()) {
                List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                bootstrapNewNodesOnHost(stack, allGatewayConfigs, nodes, allNodes);
            } else if (orchestratorType.containerOrchestrator()) {
                LOGGER.info("Skipping bootstrap of the new machines because the stack's orchestrator type is '{}'.", stackOrchestratorType);
            } else {
                LOGGER.error("Orchestrator not found: {}", stackOrchestratorType);
                throw new CloudbreakException("HostOrchestrator not found: " + stackOrchestratorType);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private void bootstrapNewNodesOnHost(Stack stack, List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, Set<Node> allNodes)
            throws CloudbreakException, CloudbreakOrchestratorException {
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Cluster cluster = stack.getCluster();
        Boolean enableKnox = cluster.getGateway().getEnableGateway();
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
            PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithTimeoutSingleFailure(
                    hostBootstrapApiCheckerTask, new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");
        }

        byte[] stateZip = null;
        ClusterComponent stateComponent = clusterComponentProvider.getComponent(cluster.getId(), ComponentType.SALT_STATE);
        if (stateComponent != null) {
            String content = (String) stateComponent.getAttributes().getMap().getOrDefault(ComponentType.SALT_STATE.name(), "");
            if (!content.isEmpty()) {
                stateZip = Base64.decodeBase64(content);
            }
        }
        hostOrchestrator.bootstrapNewNodes(allGatewayConfigs, nodes, allNodes, stateZip, clusterDeletionBasedModel(stack.getId(), null));

        InstanceMetaData primaryGateway = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, primaryGateway, enableKnox);
        PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithTimeoutSingleFailure(hostClusterAvailabilityCheckerTask,
                new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
        if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
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

    /*
     * Generate hostname for the new nodes, retain the hostname for old nodes
     * Even if the domain has changed keep the rest of the nodes domain.
     * Note: if we recovered a node the private id is not the same as it is in the hostname
     */
    private Node createNode(String customHostname, InstanceMetaData im, String domain, boolean hostgroupAsHostname) {
        String discoveryFQDN = im.getDiscoveryFQDN();
        if (isNoneBlank(discoveryFQDN)) {
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getShortHostname(), domain, im.getInstanceGroupName());
        } else {
            String hostname = hostDiscoveryService.generateHostname(customHostname, im.getInstanceGroupName(), im.getPrivateId(), hostgroupAsHostname);
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), hostname, domain, im.getInstanceGroupName());
        }
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
