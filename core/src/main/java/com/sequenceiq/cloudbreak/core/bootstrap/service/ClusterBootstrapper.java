package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.polling.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.polling.PollingResult.TIMEOUT;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostBootstrapApiCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostClusterAvailabilityCheckerTask;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ClusterBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int MAX_POLLING_ATTEMPTS = 500;

    @Inject
    private StackService stackService;

    @Inject
    private OrchestratorService orchestratorService;

    @Inject
    private PollingService<HostBootstrapApiContext> hostBootstrapApiPollingService;

    @Inject
    private HostBootstrapApiCheckerTask hostBootstrapApiCheckerTask;

    @Inject
    private PollingService<HostOrchestratorClusterContext> hostClusterAvailabilityPollingService;

    @Inject
    private HostClusterAvailabilityCheckerTask hostClusterAvailabilityCheckerTask;

    @Inject
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterNodeNameGenerator clusterNodeNameGenerator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private TransactionService transactionService;

    public void bootstrapMachines(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        bootstrapOnHost(stack);
    }

    public void reBootstrapMachines(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("ReBootstrapMachines for stack [{}] [{}]", stack.getName(), stack.getResourceCrn());
        reBootstrapOnHost(stack);
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public void bootstrapOnHost(Stack stack) throws CloudbreakException {
        bootstrapOnHostInternal(stack, this::saveSaltComponent);
    }

    private void bootstrapOnHostInternal(Stack stack, Consumer<Stack> saveOrUpdateSaltComponent) throws CloudbreakException {
        try {
            Set<Node> nodes = transactionService.required(() -> collectNodesForBootstrap(stack));
            List<GatewayConfig> allGatewayConfig = collectAndCheckGateways(stack);

            saveOrUpdateSaltComponent.accept(stack);

            BootstrapParams params = createBootstrapParams(stack);
            hostOrchestrator.bootstrap(allGatewayConfig, nodes, params, clusterDeletionBasedModel(stack.getId(), null));

            InstanceMetaData primaryGateway = stack.getPrimaryGatewayInstance();
            saveOrchestrator(stack, primaryGateway);
            checkIfAllNodesAvailable(stack, nodes, primaryGateway);
        } catch (TransactionExecutionException e) {
            throw new CloudbreakException(e.getCause());
        } catch (CloudbreakOrchestratorFailedException e) {
            checkIfAnyInstanceIsNotInStartedState(stack, e);
            throw new CloudbreakException(e);
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    private void checkIfAnyInstanceIsNotInStartedState(Stack stack, CloudbreakOrchestratorFailedException e) throws CloudbreakException {
        Set<InstanceMetaData> runningInstances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(runningInstances, stack.getEnvironmentCrn(), stack.getStackAuthentication());
        List<CloudVmInstanceStatus> instanceStatuses = stackInstanceStatusChecker.queryInstanceStatuses(stack, cloudInstances);
        List<CloudVmInstanceStatus> notStartedInstances = instanceStatuses.stream()
                .filter(instance -> !InstanceStatus.STARTED.equals(instance.getStatus()))
                .collect(Collectors.toList());
        if (!notStartedInstances.isEmpty()) {
            String notStartedInstanceIdsAndStatuses = notStartedInstances.stream()
                    .map(instance -> instance.getCloudInstance().getInstanceId() + ": " + instance.getStatus())
                    .collect(Collectors.joining(", "));
            throw new CloudbreakException("Nodes were not in started state during cluster install:  " + notStartedInstanceIdsAndStatuses
                    + " Please check " + stack.getCloudPlatform() + " logs. Original message: " + e.getMessage());
        }
    }

    public void reBootstrapOnHost(Stack stack) throws CloudbreakException {
        bootstrapOnHostInternal(stack, this::updateSaltComponent);
    }

    private void checkIfAllNodesAvailable(Stack stack, Set<Node> nodes, InstanceMetaData primaryGateway) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, primaryGateway, isKnoxEnabled(stack));
        PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(
                hostClusterAvailabilityCheckerTask, new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.");
        if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
        }
    }

    private void saveSaltComponent(Stack stack) {
        LOGGER.info("Save salt component for stack: {}", stack.getName());
        ClusterComponent saltComponent = clusterComponentProvider.getComponent(stack.getCluster().getId(), ComponentType.SALT_STATE);
        if (saltComponent == null) {
            try {
                byte[] stateConfigZip = hostOrchestrator.getStateConfigZip();
                saltComponent = createSaltComponent(stack, stateConfigZip);
                clusterComponentProvider.store(saltComponent);
            } catch (IOException e) {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private ClusterComponent createSaltComponent(Stack stack, byte[] stateConfigZip) {
        ClusterComponent saltComponent;
        saltComponent = new ClusterComponent(ComponentType.SALT_STATE,
                new Json(singletonMap(ComponentType.SALT_STATE.name(), Base64.encodeBase64String(stateConfigZip))), stack.getCluster());
        return saltComponent;
    }

    public ClusterComponent updateSaltComponent(Stack stack) {
        ClusterComponent saltComponent = clusterComponentProvider.getComponent(stack.getCluster().getId(), ComponentType.SALT_STATE);
        try {
            byte[] stateConfigZip = hostOrchestrator.getStateConfigZip();
            if (saltComponent == null) {
                LOGGER.debug("Create new salt component");
                saltComponent = createSaltComponent(stack, stateConfigZip);
            } else {
                LOGGER.debug("Overwrite existing salt component attributes");
                saltComponent.setAttributes(new Json(singletonMap(ComponentType.SALT_STATE.name(), Base64.encodeBase64String(stateConfigZip))));
            }
            return clusterComponentProvider.store(saltComponent);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    private void saveOrchestrator(Stack stack, InstanceMetaData primaryGateway) {
        String gatewayIp = gatewayConfigService.getGatewayIp(stack, primaryGateway);
        Orchestrator orchestrator = stack.getOrchestrator();
        orchestrator.setApiEndpoint(gatewayIp + ':' + stack.getGatewayPort());
        orchestrator.setType(hostOrchestrator.name());
        orchestratorService.save(orchestrator);
    }

    private BootstrapParams createBootstrapParams(Stack stack) {
        LOGGER.debug("Create bootstrap params");
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.cloudPlatform());
        try {
            Image image = componentConfigProviderService.getImage(stack.getId());
            params.setOs(image.getOs());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.warn("Image not found for stack {}", stack.getName(), e);
        }
        boolean saltBootstrapFpSupported = isSaltBootstrapFpSupported(stack);
        boolean saltBootstrapRestartNeededSupported = isSaltBootstrapRestartNeededSupported(stack);
        params.setSaltBootstrapFpSupported(saltBootstrapFpSupported);
        params.setRestartNeededFlagSupported(saltBootstrapRestartNeededSupported);
        LOGGER.debug("Created bootstrap params: {}", params);
        return params;
    }

    private boolean isSaltBootstrapRestartNeededSupported(Stack stack) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .allMatch(i -> saltBootstrapVersionChecker.isRestartNeededFlagSupported(i));
    }

    private boolean isSaltBootstrapFpSupported(Stack stack) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .allMatch(i -> saltBootstrapVersionChecker.isFingerprintingSupported(i));
    }

    private List<GatewayConfig> collectAndCheckGateways(Stack stack) {
        LOGGER.info("Collect and check gateways for {}", stack.getName());
        List<GatewayConfig> allGatewayConfig = new ArrayList<>();
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, isKnoxEnabled(stack));
            LOGGER.info("Add gateway config: {}", gatewayConfig);
            allGatewayConfig.add(gatewayConfig);
            PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithAbsoluteTimeout(
                    hostBootstrapApiCheckerTask, new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");
        }
        return allGatewayConfig;
    }

    private boolean isKnoxEnabled(Stack stack) {
        return stack.getCluster().getGateway() != null;
    }

    private Set<Node> collectNodesForBootstrap(Stack stack) {
        Set<Node> nodes = new HashSet<>();
        String domain = hostDiscoveryService.determineDomain(stack.getCustomDomain(), stack.getName(), stack.isClusterNameAsSubdomain());

        Map<String, AtomicLong> hostGroupNodeIndexes = new HashMap<>();
        Set<String> clusterNodeNames = stack.getNotTerminatedInstanceMetaDataList().stream()
                .map(InstanceMetaData::getShortHostname).collect(Collectors.toSet());

        Set<InstanceMetaData> notDeletedInstanceMetaDataSet = instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stack.getId());
        for (InstanceMetaData im : notDeletedInstanceMetaDataSet) {
            if (im.getPrivateIp() == null && im.getPublicIpWrapper() == null) {
                LOGGER.debug("Skipping instance metadata because the public ip and private ips are null '{}'.", im);
            } else {
                String generatedHostName = clusterNodeNameGenerator.getNodeNameForInstanceMetadata(im, stack, hostGroupNodeIndexes, clusterNodeNames);
                String instanceId = im.getInstanceId();
                String instanceType = im.getInstanceGroup().getTemplate().getInstanceType();
                nodes.add(new Node(im.getPrivateIp(), im.getPublicIpWrapper(), instanceId, instanceType, generatedHostName, domain, im.getInstanceGroupName()));
                initializeDiscoveryFqdnOfInstanceMetadata(im, domain, generatedHostName);
            }
        }
        instanceMetaDataService.saveAll(notDeletedInstanceMetaDataSet);
        return nodes;
    }

    public void bootstrapNewNodes(Long stackId, Set<String> upscaleCandidateAddresses) throws CloudbreakException {
        LOGGER.info("Bootstrap new nodes: {}", upscaleCandidateAddresses);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<Node> nodes = new HashSet<>();
        Set<Node> allNodes = new HashSet<>();

        try {
            transactionService.required(() -> collectNodes(stackId, upscaleCandidateAddresses, stack, nodes, allNodes));
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            cleanupOldSaltState(allGatewayConfigs, nodes);
            bootstrapNewNodesOnHost(stack, allGatewayConfigs, nodes, allNodes);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        } catch (TransactionExecutionException e) {
            throw new CloudbreakException(e.getCause());
        }
    }

    private void collectNodes(Long stackId, Set<String> upscaleCandidateAddresses, Stack stack,
            Set<Node> nodes, Set<Node> allNodes) {
        Set<InstanceMetaData> metaDataSet = instanceMetaDataService.getReachableInstanceMetadataByStackId(stackId)
                .stream()
                .filter(im -> im.getPrivateIp() != null && im.getPublicIpWrapper() != null)
                .collect(Collectors.toSet());
        String clusterDomain = getClusterDomain(metaDataSet, stack.getCustomDomain());

        Map<String, AtomicLong> hostGroupNodeIndexes = new HashMap<>();
        Set<String> clusterNodeNames = stack.getNotTerminatedInstanceMetaDataList().stream()
                .map(InstanceMetaData::getShortHostname).collect(Collectors.toSet());

        for (InstanceMetaData im : metaDataSet) {
            Node node = createNodeAndInitFqdnInInstanceMetadata(stack, im, clusterDomain, hostGroupNodeIndexes, clusterNodeNames);
            if (upscaleCandidateAddresses.contains(im.getPrivateIp())) {
                nodes.add(node);
            }
            allNodes.add(node);
        }
        instanceMetaDataService.saveAll(metaDataSet);
    }

    private void cleanupOldSaltState(List<GatewayConfig> allGatewayConfigs, Set<Node> nodes) throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> saltMastersToCorrect = allGatewayConfigs.stream()
                .filter(gc -> nodes.stream().noneMatch(n -> gc.getPrivateAddress().equals(n.getPrivateIp())))
                .collect(Collectors.toList());
        for  (GatewayConfig masterToCorrect: saltMastersToCorrect) {
            hostOrchestrator.removeDeadSaltMinions(masterToCorrect);
        }
    }

    private String getClusterDomain(Set<InstanceMetaData> metaDataSet, String customDomain) {
        if (customDomain != null && !customDomain.isEmpty()) {
            return customDomain;
        }
        Optional<InstanceMetaData> metadataWithFqdn = metaDataSet.stream().filter(im -> isNoneBlank(im.getDiscoveryFQDN())).findAny();
        if (metadataWithFqdn.isPresent()) {
            return metadataWithFqdn.get().getDomain();
        }
        throw new RuntimeException("Could not determine domain of cluster");
    }

    private void bootstrapNewNodesOnHost(Stack stack, List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, Set<Node> allNodes)
            throws CloudbreakOrchestratorException {
        Cluster cluster = stack.getCluster();
        Boolean enableKnox = cluster.getGateway() != null;
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
            PollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithAbsoluteTimeout(
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
        BootstrapParams params = createBootstrapParams(stack);

        hostOrchestrator.bootstrapNewNodes(allGatewayConfigs, nodes, allNodes, stateZip, params, clusterDeletionBasedModel(stack.getId(), null));

        InstanceMetaData primaryGateway = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, primaryGateway, enableKnox);
        PollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService
                .pollWithAbsoluteTimeout(hostClusterAvailabilityCheckerTask,
                        new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
        if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
        }
    }

    /*
     * Generate hostname for the new nodes, retain the hostname for old nodes
     * Even if the domain has changed keep the rest of the nodes domain.
     * Note: if we recovered a node the private id is not the same as it is in the hostname
     */
    private Node createNodeAndInitFqdnInInstanceMetadata(Stack stack, InstanceMetaData im, String domain, Map<String, AtomicLong> hostGroupNodeIndexes,
            Set<String> clusterNodeNames) {
        String discoveryFQDN = im.getDiscoveryFQDN();
        String instanceId = im.getInstanceId();
        String instanceType = im.getInstanceGroup().getTemplate().getInstanceType();
        if (isNoneBlank(discoveryFQDN)) {
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), instanceId, instanceType, im.getShortHostname(), domain, im.getInstanceGroupName());
        } else {
            String hostname = clusterNodeNameGenerator.getNodeNameForInstanceMetadata(im, stack, hostGroupNodeIndexes, clusterNodeNames);
            initializeDiscoveryFqdnOfInstanceMetadata(im, domain, hostname);
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), instanceId, instanceType, hostname, domain, im.getInstanceGroupName());
        }
    }

    private void initializeDiscoveryFqdnOfInstanceMetadata(InstanceMetaData im, String domain, String hostname) {
        String generatedFqdn = String.format("%s.%s", hostname, domain);
        im.setDiscoveryFQDN(generatedFqdn);
    }

    private void validatePollingResultForCancellation(PollingResult pollingResult, String cancelledMessage) {
        if (EXIT.equals(pollingResult)) {
            throw new CancellationException(cancelledMessage);
        }
    }
}
