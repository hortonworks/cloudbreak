package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.polling.PollingResult.EXIT;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
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
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ClusterBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    private static final int POLL_INTERVAL = 5000;

    private static final int MAX_POLLING_ATTEMPTS = 500;

    @Value("${info.app.version}")
    private String cbVersion;

    @Inject
    private StackDtoService stackDtoService;

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

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    public void bootstrapMachines(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        bootstrapOnHost(stackDto);
    }

    public void reBootstrapMachines(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.info("ReBootstrapMachines for stack [{}] [{}]", stackDto.getName(), stackDto.getResourceCrn());
        reBootstrapOnHost(stackDto);
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public void bootstrapOnHost(StackDto stack) throws CloudbreakException {
        bootstrapOnHostInternal(stack, this::saveSaltComponent);
    }

    private void bootstrapOnHostInternal(StackDto stack, Consumer<StackDtoDelegate> saveOrUpdateSaltComponent) throws CloudbreakException {
        try {
            Set<Node> nodes = transactionService.required(() -> collectNodesForBootstrap(stack));
            List<GatewayConfig> allGatewayConfig = collectAndCheckGateways(stack);

            saveOrUpdateSaltComponent.accept(stack);

            BootstrapParams params = createBootstrapParams(stack);
            hostOrchestrator.bootstrap(allGatewayConfig, nodes, params, clusterDeletionBasedModel(stack.getId(), null));

            InstanceMetadataView primaryGateway = stack.getPrimaryGatewayInstance();
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

    private void checkIfAnyInstanceIsNotInStartedState(StackDtoDelegate stack, CloudbreakOrchestratorFailedException e) throws CloudbreakException {
        List<InstanceGroupDto> instanceGroupDtos = stack.getInstanceGroupDtos();
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(instanceGroupDtos, stack.getEnvironmentCrn(), stack.getStackAuthentication());
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

    public void reBootstrapOnHost(StackDto stackDto) throws CloudbreakException {
        bootstrapOnHostInternal(stackDto, this::updateSaltComponent);
    }

    private void checkIfAllNodesAvailable(StackDto stack, Set<Node> nodes, InstanceMetadataView primaryGateway)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), primaryGateway, isKnoxEnabled(stack));
        ExtendedPollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService.pollWithAbsoluteTimeout(
                hostClusterAvailabilityCheckerTask, new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes),
                POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling.getPollingResult(), "Polling of all nodes availability was cancelled.");
        if (allNodesAvailabilityPolling.isTimeout()) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
        }
    }

    private void saveSaltComponent(StackDtoDelegate stack) {
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

    private ClusterComponent createSaltComponent(StackDtoDelegate stack, byte[] stateConfigZip) {
        Cluster clusterReference = clusterService.getClusterReference(stack.getCluster().getId());
        return new ClusterComponent(ComponentType.SALT_STATE,
                new Json(Map.of(ComponentType.SALT_STATE.name(), Base64.encodeBase64String(stateConfigZip),
                        ClusterComponent.CB_VERSION_KEY, cbVersion)), clusterReference);
    }

    public ClusterComponent updateSaltComponent(StackDtoDelegate stackDto) {
        try {
            byte[] stateConfigZip = hostOrchestrator.getStateConfigZip();
            return updateSaltComponent(stackDto, stateConfigZip);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    public ClusterComponent updateSaltComponent(StackDtoDelegate stack, byte[] stateConfigZip) {
        ClusterComponent saltComponent = clusterComponentProvider.getComponent(stack.getCluster().getId(), ComponentType.SALT_STATE);
        if (saltComponent == null) {
            LOGGER.debug("Create new salt component");
            saltComponent = createSaltComponent(stack, stateConfigZip);
        } else {
            LOGGER.debug("Overwrite existing salt component attributes");
            saltComponent.setAttributes(new Json(Map.of(ComponentType.SALT_STATE.name(), Base64.encodeBase64String(stateConfigZip),
                    ClusterComponent.CB_VERSION_KEY, cbVersion)));
        }
        return clusterComponentProvider.store(saltComponent);
    }

    private void saveOrchestrator(StackDtoDelegate stack, InstanceMetadataView primaryGateway) {
        String gatewayIp = gatewayConfigService.getGatewayIp(stack.getSecurityConfig(), primaryGateway);
        Orchestrator orchestrator = stack.getOrchestrator();
        orchestrator.setApiEndpoint(gatewayIp + ':' + stack.getGatewayPort());
        orchestrator.setType(hostOrchestrator.name());
        orchestratorService.save(orchestrator);
    }

    private BootstrapParams createBootstrapParams(StackDtoDelegate stack) {
        LOGGER.debug("Create bootstrap params");
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.getCloudPlatform());
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

    private boolean isSaltBootstrapRestartNeededSupported(StackDtoDelegate stack) {
        return stack.getAllAvailableInstances().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(i -> saltBootstrapVersionChecker.isRestartNeededFlagSupported(i));
    }

    private boolean isSaltBootstrapFpSupported(StackDtoDelegate stack) {
        return stack.getAllAvailableInstances().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(i -> saltBootstrapVersionChecker.isFingerprintingSupported(i));
    }

    private List<GatewayConfig> collectAndCheckGateways(StackDtoDelegate stack) {
        LOGGER.info("Collect and check gateways for {}", stack.getName());
        List<GatewayConfig> allGatewayConfig = new ArrayList<>();
        for (InstanceMetadataView gateway : stack.getAllAvailableGatewayInstances()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), gateway, isKnoxEnabled(stack));
            LOGGER.info("Add gateway config: {}", gatewayConfig);
            allGatewayConfig.add(gatewayConfig);
            ExtendedPollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithAbsoluteTimeout(
                    hostBootstrapApiCheckerTask, new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling.getPollingResult(), "Polling of bootstrap API was cancelled.");
        }
        return allGatewayConfig;
    }

    private boolean isKnoxEnabled(StackDtoDelegate stack) {
        return stack.hasGateway();
    }

    private Set<Node> collectNodesForBootstrap(StackDtoDelegate stack) {
        Set<Node> nodes = new HashSet<>();
        String domain = hostDiscoveryService.determineDomain(stack.getCustomDomain(), stack.getName(), stack.isClusterNameAsSubdomain());

        Map<String, AtomicLong> hostGroupNodeIndexes = new HashMap<>();
        Set<String> clusterNodeNames = stack.getNotTerminatedInstanceMetaData().stream()
                .map(InstanceMetadataView::getShortHostname).collect(Collectors.toSet());

        // Ordered list of metadata to guarantee consistent hostname generation across multiple cluster recoveries
        List<InstanceMetaData> notDeletedInstanceMetaDataSet = instanceMetaDataService.findNotTerminatedAsOrderedListForStack(stack.getId());
        LOGGER.debug("There are the following available instances: {}", notDeletedInstanceMetaDataSet);
        for (InstanceMetaData im : notDeletedInstanceMetaDataSet) {
            if (im.getPrivateIp() == null && im.getPublicIpWrapper() == null) {
                LOGGER.debug("Skipping instance metadata because the public ip and private ips are null '{}'.", im);
            } else {
                String generatedHostName = clusterNodeNameGenerator.getNodeNameForInstanceMetadata(im, stack.getStack(), hostGroupNodeIndexes, clusterNodeNames);
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
        StackDto stack = stackDtoService.getById(stackId);
        Set<Node> nodes = new HashSet<>();
        Set<Node> allNodes = new HashSet<>();

        try {
            collectNodes(upscaleCandidateAddresses, stack, nodes, allNodes);
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            cleanupOldSaltState(allGatewayConfigs, nodes);
            bootstrapNewNodesOnHost(stack, allGatewayConfigs, nodes, allNodes);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private void collectNodes(Set<String> upscaleCandidateAddresses, StackDto stack,
            Set<Node> nodes, Set<Node> allNodes) {
        Set<InstanceMetaData> metaDataSet = instanceMetaDataService.getReachableInstanceMetadataByStackId(stack.getId())
                .stream()
                .filter(im -> im.getPrivateIp() != null && im.getPublicIpWrapper() != null)
                .collect(Collectors.toSet());
        String clusterDomain = getClusterDomain(metaDataSet, stack.getCustomDomain());

        LOGGER.info("Cluster domain: {}", clusterDomain);

        Map<String, AtomicLong> hostGroupNodeIndexes = new HashMap<>();
        Set<String> clusterNodeNames = stack.getNotTerminatedInstanceMetaData().stream()
                .map(InstanceMetadataView::getShortHostname).collect(Collectors.toSet());

        LOGGER.info("Cluster node names: {}", clusterNodeNames);

        for (InstanceMetaData im : metaDataSet) {
            Node node = createNodeAndInitFqdnInInstanceMetadata(stack, im, clusterDomain, hostGroupNodeIndexes, clusterNodeNames);
            if (upscaleCandidateAddresses.contains(im.getPrivateIp())) {
                LOGGER.info("Node is an upscale candidate: {}", node.getInstanceId());
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
            LOGGER.info("Remove dead salt minions on master: {}", masterToCorrect);
            hostOrchestrator.removeDeadSaltMinions(masterToCorrect);
        }
    }

    private String getClusterDomain(Set<InstanceMetaData> metaDataSet, String customDomain) {
        if (customDomain != null && !customDomain.isEmpty()) {
            LOGGER.info("Custom domain for cluster: {}", customDomain);
            return customDomain;
        }
        Optional<InstanceMetaData> metadataWithFqdn = metaDataSet.stream().filter(im -> isNoneBlank(im.getDiscoveryFQDN())).findAny();
        if (metadataWithFqdn.isPresent()) {
            return metadataWithFqdn.get().getDomain();
        }
        throw new RuntimeException("Could not determine domain of cluster");
    }

    private void bootstrapNewNodesOnHost(StackDto stack, List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, Set<Node> allNodes)
            throws CloudbreakOrchestratorException {
        LOGGER.info("Bootstrap new nodes: {}", nodes);
        ClusterView cluster = stack.getCluster();
        Boolean enableKnox = stack.hasGateway();
        for (InstanceMetadataView gateway : stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), gateway, enableKnox);
            ExtendedPollingResult bootstrapApiPolling = hostBootstrapApiPollingService.pollWithAbsoluteTimeout(
                    hostBootstrapApiCheckerTask, new HostBootstrapApiContext(stack, gatewayConfig, hostOrchestrator), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling.getPollingResult(), "Polling of bootstrap API was cancelled.");
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

        InstanceMetadataView primaryGateway = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack.getStack(), stack.getSecurityConfig(), primaryGateway, enableKnox);
        ExtendedPollingResult allNodesAvailabilityPolling = hostClusterAvailabilityPollingService
                .pollWithAbsoluteTimeout(hostClusterAvailabilityCheckerTask,
                        new HostOrchestratorClusterContext(stack, hostOrchestrator, gatewayConfig, nodes), POLL_INTERVAL, MAX_POLLING_ATTEMPTS);
        validatePollingResultForCancellation(allNodesAvailabilityPolling.getPollingResult(), "Polling of new nodes availability was cancelled.");
        if (allNodesAvailabilityPolling.isTimeout()) {
            clusterBootstrapperErrorHandler.terminateFailedNodes(hostOrchestrator, null, stack, gatewayConfig, nodes);
        }
    }

    /*
     * Generate hostname for the new nodes, retain the hostname for old nodes
     * Even if the domain has changed keep the rest of the nodes domain.
     * Note: if we recovered a node the private id is not the same as it is in the hostname
     */
    private Node createNodeAndInitFqdnInInstanceMetadata(StackDto stack, InstanceMetaData im, String domain, Map<String, AtomicLong> hostGroupNodeIndexes,
            Set<String> clusterNodeNames) {
        String discoveryFQDN = im.getDiscoveryFQDN();
        String instanceId = im.getInstanceId();
        String instanceType = im.getInstanceGroup().getTemplate().getInstanceType();
        LOGGER.info("Create and init FQDN for instance if necessary: {}", im);
        if (isNoneBlank(discoveryFQDN)) {
            LOGGER.info("FQDN in not null for instance with id: {}", instanceId);
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), instanceId, instanceType, im.getShortHostname(), domain, im.getInstanceGroupName());
        } else {
            LOGGER.info("FQDN is blank, generate FQDN for {}", instanceId);
            String hostname = clusterNodeNameGenerator.getNodeNameForInstanceMetadata(im, stack.getStack(), hostGroupNodeIndexes, clusterNodeNames);
            LOGGER.info("Generated hostname for {}: {}", instanceId, hostname);
            initializeDiscoveryFqdnOfInstanceMetadata(im, domain, hostname);
            return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), instanceId, instanceType, hostname, domain, im.getInstanceGroupName());
        }
    }

    private void initializeDiscoveryFqdnOfInstanceMetadata(InstanceMetaData im, String domain, String hostname) {
        String generatedFqdn = String.format("%s.%s", hostname, domain);
        LOGGER.info("Set generated FQDN for {}: {}", im.getInstanceId(), generatedFqdn);
        im.setDiscoveryFQDN(generatedFqdn);
    }

    private void validatePollingResultForCancellation(PollingResult pollingResult, String cancelledMessage) {
        if (EXIT.equals(pollingResult)) {
            throw new CancellationException(cancelledMessage);
        }
    }

    public void validateRotateSaltPassword(StackDto stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException("Rotating SaltStack user password is only available for stacks in available status");
        }
        if (!isChangeSaltuserPasswordSupported(stack)) {
            throw new BadRequestException(String.format("Rotating SaltStack user password is not supported with your image version, " +
                            "please upgrade to an image with salt-bootstrap version >= %s (you can find this information in the image catalog)",
                    SaltBootstrapVersionChecker.CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION));
        }
    }

    private boolean isChangeSaltuserPasswordSupported(StackDto stack) {
        return stack.getAllAvailableGatewayInstances().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(image -> saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(image));
    }

    public void rotateSaltPassword(StackDto stack) throws CloudbreakOrchestratorException {
        validateRotateSaltPassword(stack);
        SecurityConfig securityConfig = securityConfigService.getOneByStackId(stack.getId());
        String oldPassword = securityConfig.getSaltSecurityConfig().getSaltPassword();
        String newPassword = PasswordUtil.generatePassword();
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        hostOrchestrator.changePassword(allGatewayConfig, newPassword, oldPassword);
        securityConfigService.changeSaltPassword(securityConfig, newPassword);
    }
}
