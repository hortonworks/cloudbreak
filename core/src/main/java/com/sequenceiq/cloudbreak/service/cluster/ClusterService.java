package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPO_ID_TAG;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_AUTORECOVERY_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_REPORTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_HOST_STATUS_UPDATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_RECOVERED_NODES_REPORTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_IGNORED;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState.ClusterManagerStatus;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    private static final String MASTER_CATEGORY = "MASTER";

    private static final String RECOVERY = "RECOVERY";

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterRepository repository;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private BlueprintValidatorFactory blueprintValidatorFactory;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    private UsageLoggingUtil usageLoggingUtil;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Measure(ClusterService.class)
    public Cluster create(Stack stack, Cluster cluster, List<ClusterComponent> components, User user) throws TransactionExecutionException {
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        String stackName = stack.getName();
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster().getName()));
        }
        return transactionService.required(() -> {
            setWorkspace(cluster, stack.getWorkspace());
            cluster.setEnvironmentCrn(stack.getEnvironmentCrn());

            long start = System.currentTimeMillis();
            if (repository.findByNameAndWorkspace(cluster.getName(), stack.getWorkspace()).isPresent()) {
                throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName());
            }
            LOGGER.debug("Cluster name collision check took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            if (Status.CREATE_FAILED.equals(stack.getStatus())) {
                throw new BadRequestException("Stack creation failed, cannot create cluster.");
            }

            start = System.currentTimeMillis();
            LOGGER.debug("Host group constrainst saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            if (cluster.getFileSystem() != null) {
                cluster.setFileSystem(fileSystemConfigService.createWithMdcContextRestore(cluster.getFileSystem(), cluster.getWorkspace(), user));
            }
            LOGGER.debug("Filesystem config saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            removeGatewayIfNotSupported(cluster, components);

            cluster.setStack(stack);
            stack.setCluster(cluster);

            Cluster savedCluster = saveClusterAndComponent(cluster, components, stackName);
            usageLoggingUtil.logClusterRequestedUsageEvent(cluster);
            if (stack.isAvailable()) {
                flowManager.triggerClusterInstall(stack.getId());
                InMemoryStateStore.putCluster(savedCluster.getId(), statusToPollGroupConverter.convert(savedCluster.getStatus()));
                if (InMemoryStateStore.getStack(stack.getId()) == null) {
                    InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
                }
            }
            return savedCluster;
        });
    }

    private void setWorkspace(Cluster cluster, Workspace workspace) {
        cluster.setWorkspace(workspace);
        if (cluster.getGateway() != null) {
            cluster.getGateway().setWorkspace(workspace);
        }
    }

    private Cluster saveClusterAndComponent(Cluster cluster, List<ClusterComponent> components, String stackName) {
        Cluster savedCluster;
        try {
            long start = System.currentTimeMillis();
            savedCluster = repository.save(cluster);
            if (savedCluster.getGateway() != null) {
                gatewayService.save(savedCluster.getGateway());
            }
            LOGGER.debug("Cluster object saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);
            clusterComponentConfigProvider.store(components, savedCluster);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CLUSTER, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedCluster;
    }

    private void removeGatewayIfNotSupported(Cluster cluster, List<ClusterComponent> components) {
        Optional<ClusterComponent> cmRepoOpt = components.stream().filter(cmp -> ComponentType.CM_REPO_DETAILS.equals(cmp.getComponentType())).findFirst();
        if (cmRepoOpt.isPresent()) {
            try {
                ClouderaManagerRepo cmRepo = cmRepoOpt.get().getAttributes().get(ClouderaManagerRepo.class);
                if (!CMRepositoryVersionUtil.isKnoxGatewaySupported(cmRepo)) {
                    LOGGER.debug("Knox gateway is not supported by CM version: {}, ignoring it for cluster: {}", cmRepo.getVersion(), cluster.getName());
                    cluster.setGateway(null);
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read CM repo cluster component", e);
            }
        }
    }

    public Cluster saveWithRef(Cluster cluster) {
        Cluster savedCluster;
        try {
            long start = System.currentTimeMillis();
            if (cluster.getFileSystem() != null) {
                cluster.getFileSystem().setWorkspace(cluster.getWorkspace());
                fileSystemConfigService.pureSave(cluster.getFileSystem());
            }
            savedCluster = save(cluster);
            Gateway gateway = cluster.getGateway();
            if (gateway != null) {
                gateway.setCluster(savedCluster);
                gatewayService.save(gateway);
            }
            List<ClusterComponent> store = clusterComponentConfigProvider.store(cluster.getComponents(), savedCluster);
            savedCluster.setComponents(new HashSet<>(store));
            LOGGER.info("Cluster object saved in {} ms with cluster id {}", System.currentTimeMillis() - start, cluster.getId());
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CLUSTER, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedCluster;
    }

    public boolean isSingleNode(Stack stack) {
        int nodeCount = 0;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            nodeCount += ig.getNodeCount();
        }
        return nodeCount == 1;
    }

    public Iterable<Cluster> saveAll(Iterable<Cluster> clusters) {
        return repository.saveAll(clusters);
    }

    public Cluster save(Cluster cluster) {
        return repository.save(cluster);
    }

    public void delete(Long stackId, boolean forced) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        LOGGER.debug("Cluster delete requested.");
        markVolumesForDeletion(stack);
        flowManager.triggerClusterTermination(stack, forced, ThreadBasedUserCrnProvider.getUserCrn());
    }

    private void markVolumesForDeletion(Stack stack) {
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }
        LOGGER.debug("Mark volumes for delete on termination in case of active repair flow.");
        try {
            transactionService.required(() -> {
                List<Resource> resources = stack.getResourcesByType(ResourceType.AWS_ENCRYPTED_VOLUME);
                resources.forEach(resource -> updateDeleteVolumesFlag(Boolean.TRUE, resource));
                return resourceService.saveAll(resources);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Optional<Cluster> retrieveClusterByStackIdWithoutAuth(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

    public Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig) {
        Cluster cluster = getCluster(clusterId);
        cluster.setClusterManagerIp(ambariClientConfig.getApiAddress());
        cluster = repository.save(cluster);
        LOGGER.info("Updated cluster: [ambariIp: '{}'].", ambariClientConfig.getApiAddress());
        return cluster;
    }

    public void updateInstancesToRunning(Long clusterId, Map<String, List<String>> hostsPerHostGroup) {
        try {
            transactionService.required(() -> {
                for (Entry<String, List<String>> hostGroupEntry : hostsPerHostGroup.entrySet()) {
                    hostGroupService.getByClusterIdAndName(clusterId, hostGroupEntry.getKey()).ifPresent(hostGroup -> {
                        hostGroup.getInstanceGroup().getUnattachedInstanceMetaDataSet()
                                .forEach(instanceMetaData -> {
                                    instanceMetaData.setInstanceStatus(SERVICES_RUNNING);
                                    instanceMetaDataService.save(instanceMetaData);
                                });
                    });
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public String getStackRepositoryJson(Long stackId) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(cluster.getId());
        String stackRepoId = repoDetails.getStack().get(REPO_ID_TAG);
        return clusterApiConnectors.getConnector(stack).clusterModificationService().getStackRepositoryJson(repoDetails, stackRepoId);
    }

    public void updateHosts(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        boolean downscaleRequest = validateRequest(stack, hostGroupAdjustment);
        if (downscaleRequest) {
            updateClusterStatusByStackId(stackId, UPDATE_REQUESTED);
            flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment);
        } else {
            flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment);
        }
    }

    public void updateStatus(Long stackId, StatusRequest statusRequest) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        updateStatus(stack, statusRequest);
    }

    public void updateStatus(Stack stack, StatusRequest statusRequest) {
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stack.getName()));
        }
        switch (statusRequest) {
            case SYNC:
                sync(stack);
                break;
            case STOPPED:
                stop(stack, cluster);
                break;
            case STARTED:
                start(stack, cluster);
                break;
            default:
                throw new BadRequestException("Cannot update the status of cluster because status request not valid");
        }
    }

    public void updateUserNamePassword(Long stackId, UserNamePasswordV4Request userNamePasswordJson) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        String oldUserName = cluster.getUserName();
        String oldPassword = cluster.getPassword();
        String newUserName = userNamePasswordJson.getUserName();
        String newPassword = userNamePasswordJson.getPassword();
        if (!newUserName.equals(oldUserName)) {
            flowManager.triggerClusterCredentialReplace(stack.getId(), userNamePasswordJson.getUserName(), userNamePasswordJson.getPassword());
        } else if (!newPassword.equals(oldPassword)) {
            flowManager.triggerClusterCredentialUpdate(stack.getId(), userNamePasswordJson.getPassword());
        } else {
            throw new BadRequestException("The request may not change credential");
        }
    }

    public void reportHealthChange(String crn, Set<String> failedNodes, Set<String> newHealthyNodes) {
        if (!Sets.intersection(failedNodes, newHealthyNodes).isEmpty()) {
            throw new BadRequestException("Failed nodes " + failedNodes + " and healthy nodes " + newHealthyNodes + " should not have common items.");
        }
        try {
            transactionService.required(() -> {
                Stack stack = stackService.findByCrn(crn);
                if (stack != null && !stack.getStatus().isInProgress()) {
                    handleHealthChange(failedNodes, newHealthyNodes, stack);
                } else {
                    LOGGER.debug("Stack [{}] status is {}, thus we do not handle failure report.", stack.getName(), stack.getStatus());
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void handleHealthChange(Set<String> failedNodes, Set<String> newHealthyNodes, Stack stack) {
        Cluster cluster = stack.getCluster();
        Map<String, List<String>> autoRecoveryNodesMap = new HashMap<>();
        Map<String, InstanceMetaData> autoRecoveryMetadata = new HashMap<>();
        Map<String, InstanceMetaData> failedMetaData = new HashMap<>();
        for (String failedNode : failedNodes) {
            instanceMetaDataService.findHostInStack(stack.getId(), failedNode).ifPresentOrElse(instanceMetaData -> {
                hostGroupService.getByClusterIdAndName(cluster.getId(), instanceMetaData.getInstanceGroupName()).ifPresent(hostGroup -> {
                    if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                        validateRepair(stack, instanceMetaData);
                    }
                    String hostGroupName = hostGroup.getName();
                    if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                        prepareForAutoRecovery(stack, autoRecoveryNodesMap, autoRecoveryMetadata, failedNode, instanceMetaData, hostGroupName);
                    } else if (hostGroup.getRecoveryMode() == RecoveryMode.MANUAL) {
                        failedMetaData.put(failedNode, instanceMetaData);
                    }
                });
            }, () -> LOGGER.error("No metadata information for the node: " + failedNode));
        }
        handleChangedHosts(cluster, newHealthyNodes, autoRecoveryNodesMap, autoRecoveryMetadata, failedMetaData);
    }

    private void handleChangedHosts(Cluster cluster, Set<String> newHealthyNodes,
            Map<String, List<String>> autoRecoveryNodesMap, Map<String, InstanceMetaData> autoRecoveryHostMetadata,
            Map<String, InstanceMetaData> failedHostMetadata) {
        try {
            boolean hasAutoRecoverableNodes = !autoRecoveryNodesMap.isEmpty();
            if (hasAutoRecoverableNodes) {
                flowManager.triggerClusterRepairFlow(cluster.getStack().getId(), autoRecoveryNodesMap, false);
                updateChangedHosts(cluster, autoRecoveryHostMetadata.keySet(), Set.of(SERVICES_HEALTHY), InstanceStatus.WAITING_FOR_REPAIR,
                        CLUSTER_AUTORECOVERY_REQUESTED);
            }
            if (!failedHostMetadata.isEmpty()) {
                updateChangedHosts(cluster, failedHostMetadata.keySet(), Set.of(SERVICES_HEALTHY, SERVICES_RUNNING), SERVICES_UNHEALTHY,
                        CLUSTER_FAILED_NODES_REPORTED);
            }
            if (!newHealthyNodes.isEmpty()) {
                updateChangedHosts(cluster, newHealthyNodes, Set.of(SERVICES_UNHEALTHY, SERVICES_RUNNING), SERVICES_HEALTHY,
                        CLUSTER_RECOVERED_NODES_REPORTED);
            }
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void prepareForAutoRecovery(Stack stack,
            Map<String, List<String>> autoRecoveryNodesMap,
            Map<String, InstanceMetaData> autoRecoveryHostMetadata,
            String failedNode,
            InstanceMetaData hostMetadata,
            String hostGroupName) {
        List<String> nodeList = autoRecoveryNodesMap.get(hostGroupName);
        if (nodeList == null) {
            validateComponentsCategory(stack, hostGroupName);
            nodeList = new ArrayList<>();
            autoRecoveryNodesMap.put(hostGroupName, nodeList);
        }
        nodeList.add(failedNode);
        autoRecoveryHostMetadata.put(failedNode, hostMetadata);
    }

    public void cleanupCluster(final Long stackId) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                if (StringUtils.isEmpty(stack.getCluster().getClusterManagerIp())) {
                    LOGGER.debug("Cluster server IP was not set before, cleanup cluster operation can be skipped.");
                } else {
                    Telemetry telemetry = componentConfigProviderService.getTelemetry(stackId);
                    try {
                        clusterApiConnectors.getConnector(stack).clusterModificationService().cleanupCluster(telemetry);
                        altusMachineUserService.clearFluentMachineUser(stack, telemetry);
                    } catch (CloudbreakException e) {
                        LOGGER.error("Cluster specific cleanup failed.", e);
                    }
                }
                return stack;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void validateRepair(Stack stack, InstanceMetaData instanceMetaData) {
        if (isGateway(instanceMetaData) && withEmbeddedClusterManagerDB(stack.getCluster())) {
            throw new BadRequestException("Cluster manager server failure with embedded database cannot be repaired!");
        }
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    private boolean isGateway(InstanceMetaData instanceMetaData) {
        return instanceMetaData.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY;
    }

    private boolean withEmbeddedClusterManagerDB(Cluster cluster) {
        DatabaseType databaseType = DatabaseType.CLOUDERA_MANAGER;
        RDSConfig rdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), databaseType);
        return (rdsConfig == null || ResourceStatus.DEFAULT == rdsConfig.getStatus()) && cluster.getDatabaseServerCrn() == null;
    }

    private void updateChangedHosts(Cluster cluster, Set<String> hostNames, Set<InstanceStatus> expectedState,
            InstanceStatus newState, ResourceEvent resourceEvent) throws TransactionExecutionException {
        String recoveryMessage = cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), hostNames);
        Set<InstanceMetaData> notTerminatedInstanceMetaDatasForStack = instanceMetaDataService.findNotTerminatedForStack(cluster.getStack().getId());
        Collection<InstanceMetaData> changedHosts = new HashSet<>();
        transactionService.required(() -> {
            for (InstanceMetaData host : notTerminatedInstanceMetaDatasForStack) {
                if (expectedState.contains(host.getInstanceStatus()) && hostNames.contains(host.getDiscoveryFQDN())) {
                    host.setInstanceStatus(newState);
                    host.setStatusReason(recoveryMessage);
                    changedHosts.add(host);
                }
            }
            if (!changedHosts.isEmpty()) {
                LOGGER.info(recoveryMessage);
                String eventType;
                if (SERVICES_HEALTHY.equals(newState)) {
                    eventType = AVAILABLE.name();
                } else {
                    eventType = RECOVERY;
                }
                eventService.fireCloudbreakEvent(cluster.getStack().getId(), eventType, resourceEvent, List.of(String.join(",", hostNames)));
                instanceMetaDataService.saveAll(changedHosts);
            }
            return null;
        });
    }

    private void sync(Stack stack) {
        flowManager.triggerClusterSync(stack.getId());
    }

    private void start(Stack stack, Cluster cluster) {
        if (stack.isStartInProgress()) {
            eventService.fireCloudbreakEvent(stack.getId(), START_REQUESTED.name(), CLUSTER_START_REQUESTED);
            updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        } else {
            if (cluster.isAvailable()) {
                eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), CLUSTER_START_IGNORED);
            } else if (!cluster.isClusterReadyForStart() && !cluster.isStartFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because it isn't in STOPPED state.", cluster.getId()));
            } else if (!stack.isAvailable() && !cluster.isStartFailed()) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.getId()));
            } else {
                updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
                flowManager.triggerClusterStart(stack.getId());
            }
        }
    }

    private void stop(Stack stack, Cluster cluster) {
        StopRestrictionReason reason = stack.isInfrastructureStoppable();
        if (cluster.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), CLUSTER_STOP_IGNORED);
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    String.format("Cannot stop a cluster '%s'. Reason: %s", cluster.getId(), reason.getReason()));
        } else if (!cluster.isClusterReadyForStop() && !cluster.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STOPPED, because it isn't in AVAILABLE state.", cluster.getId()));
        } else if (!stack.isStackReadyForStop() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.getId()));
        } else if (cluster.isAvailable() || cluster.isStopFailed()) {
            updateClusterStatusByStackId(stack.getId(), STOP_REQUESTED);
            flowManager.triggerClusterStop(stack.getId());
        }
    }

    public Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason);
        StackStatus stackStatus = stackService.getCurrentStatusByStackId(stackId);
        Optional<Cluster> cluster = retrieveClusterByStackIdWithoutAuth(stackId);
        Optional<Status> clusterOldStatus = cluster.map(Cluster::getStatus);
        cluster = cluster.map(c -> {
            c.setStatus(status);
            c.setStatusReason(statusReason);
            return c;
        }).map(repository::save);
        handleInMemoryState(stackId, stackStatus, cluster, clusterOldStatus);
        return cluster.orElse(null);
    }

    private void handleInMemoryState(Long stackId, StackStatus stackStatus, Optional<Cluster> cluster, Optional<Status> clusterOldStatus) {
        cluster.ifPresent(c -> {
            clusterOldStatus.ifPresent(oldstatus -> usageLoggingUtil.logClusterStatusChangeUsageEvent(oldstatus, c));
            if (c.getStatus().isRemovableStatus()) {
                InMemoryStateStore.deleteCluster(c.getId());
                if (stackStatus.getStatus().isRemovableStatus()) {
                    InMemoryStateStore.deleteStack(stackId);
                }
            } else {
                InMemoryStateStore.putCluster(c.getId(), statusToPollGroupConverter.convert(c.getStatus()));
                if (InMemoryStateStore.getStack(stackId) == null) {
                    InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(stackStatus.getStatus()));
                }
            }
        });
    }

    public Cluster updateClusterStatusByStackId(Long stackId, Status status) {
        return updateClusterStatusByStackId(stackId, status, "");
    }

    public Cluster updateClusterStatusByStackIdOutOfTransaction(Long stackId, Status status) throws TransactionExecutionException {
        return transactionService.notSupported(() -> updateClusterStatusByStackId(stackId, status, ""));
    }

    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = repository.save(cluster);
        return cluster;
    }

    public Cluster updateCreationDateOnCluster(Cluster cluster) {
        if (cluster.getCreationStarted() == null) {
            cluster.setCreationStarted(new Date().getTime());
            cluster = updateCluster(cluster);
        }
        return cluster;
    }

    public Cluster updateClusterMetadata(Long stackId) {
        Stack stack = stackService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<InstanceMetaData> notTerminatedInstanceMetaDatas = instanceMetaDataService.findNotTerminatedForStack(stackId);
        if (!connector.clusterStatusService().isClusterManagerRunning()) {
            InstanceMetaData cmInstance = updateClusterManagerHostStatus(notTerminatedInstanceMetaDatas);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(cmInstance.getDiscoveryFQDN(), cmInstance.getInstanceStatus().name()));
            return stack.getCluster();
        } else {
            Map<HostName, ClusterManagerState> hostStatuses = connector.clusterStatusService().getExtendedHostStatuses();
            try {
                return transactionService.required(() -> {
                    List<InstanceMetaData> updatedInstanceMetaData = updateInstanceStatuses(notTerminatedInstanceMetaDatas, hostStatuses);
                    instanceMetaDataService.saveAll(updatedInstanceMetaData);
                    fireHostStatusUpdateNotification(stack, updatedInstanceMetaData);
                    return stack.getCluster();
                });
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        }
    }

    private InstanceMetaData updateClusterManagerHostStatus(Set<InstanceMetaData> notTerminatedInstanceMetaDatas) {
        InstanceMetaData cmInstance = notTerminatedInstanceMetaDatas.stream().filter(InstanceMetaData::getClusterManagerServer).findFirst()
                .orElseThrow(() -> new CloudbreakServiceException("Cluster manager inaccessible, and the corresponding instance metadata not found."));
        cmInstance.setInstanceStatus(SERVICES_UNHEALTHY);
        cmInstance.setStatusReason("Cluster manager inaccessible.");
        instanceMetaDataService.save(cmInstance);
        return cmInstance;
    }

    private void fireHostStatusUpdateNotification(Stack stack, List<InstanceMetaData> updatedInstanceMetaData) {
        updatedInstanceMetaData.forEach(instanceMetaData -> {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), CLUSTER_HOST_STATUS_UPDATED,
                    Arrays.asList(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getInstanceStatus().name()));
        });
    }

    private List<InstanceMetaData> updateInstanceStatuses(Set<InstanceMetaData> notTerminatedInstanceMetaDatas, Map<HostName,
            ClusterManagerState> hostStatuses) {
        return notTerminatedInstanceMetaDatas.stream()
                .filter(instanceMetaData -> SERVICES_RUNNING.equals(instanceMetaData.getInstanceStatus()))
                .map(instanceMetaData -> {
                    ClusterManagerState clusterManagerState = hostStatuses.get(hostName(instanceMetaData.getDiscoveryFQDN()));
                    InstanceStatus newState = ClusterManagerStatus.HEALTHY.equals(clusterManagerState.getClusterManagerStatus()) ?
                            SERVICES_HEALTHY : SERVICES_UNHEALTHY;
                    instanceMetaData.setInstanceStatus(newState);
                    instanceMetaData.setStatusReason(clusterManagerState.getStatusReason());
                    return instanceMetaData;
                }).collect(Collectors.toList());
    }

    public Cluster recreate(Stack stack, String blueprintName, Set<HostGroup> hostGroups, boolean validateBlueprint,
            StackRepoDetails stackRepoDetails) throws TransactionExecutionException {
        return transactionService.required(() -> {
            checkBlueprintIdAndHostGroups(blueprintName, hostGroups);
            Stack stackWithLists = stackService.getByIdWithListsInTransaction(stack.getId());
            Cluster cluster = getCluster(stackWithLists);
            Blueprint blueprint = blueprintService.getByNameForWorkspace(blueprintName, stack.getWorkspace());
            if (!withEmbeddedClusterManagerDB(cluster)) {
                throw new BadRequestException("Cluster Manager doesn't support resetting external DB automatically. To reset Cluster Manager schema you "
                        + "must first drop and then create it using DDL scripts from /var/lib/ambari-server/resources or /opt/cloudera/cm/schema/postgresql/");
            }
            BlueprintValidator blueprintValidator = blueprintValidatorFactory.createBlueprintValidator(blueprint);
            blueprintValidator.validate(blueprint, hostGroups, stackWithLists.getInstanceGroups(), validateBlueprint);
            boolean containerOrchestrator;
            try {
                containerOrchestrator = orchestratorTypeResolver.resolveType(stackWithLists.getOrchestrator()).containerOrchestrator();
            } catch (CloudbreakException ignored) {
                containerOrchestrator = false;
            }
            if (containerOrchestrator) {
                clusterTerminationService.deleteClusterComponents(cluster.getId());
                cluster = getCluster(stackWithLists);
            }

            try {
                cluster = prepareCluster(hostGroups, blueprint, stackWithLists, cluster);
                triggerClusterInstall(stackWithLists, cluster);
            } catch (CloudbreakException e) {
                throw new CloudbreakServiceException(e);
            }
            return stackWithLists.getCluster();
        });
    }

    private void checkBlueprintIdAndHostGroups(String blueprint, Set<HostGroup> hostGroups) {
        if (blueprint == null || hostGroups == null) {
            throw new BadRequestException("Cluster definition id and hostGroup assignments can not be null.");
        }
    }

    private Cluster prepareCluster(Collection<HostGroup> hostGroups, Blueprint blueprint, Stack stack,
            Cluster cluster) {
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().clear();
        cluster.getHostGroups().addAll(hostGroups);
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        cluster.setStatus(REQUESTED);
        cluster.setStack(stack);
        cluster = repository.save(cluster);
        return cluster;
    }

    private Cluster getCluster(Stack stack) {
        return getCluster(stack.getCluster().getId());
    }

    private Cluster getCluster(Long clusterId) {
        return repository.findById(clusterId)
                .orElseThrow(notFound("Cluster", clusterId));
    }

    private void triggerClusterInstall(Stack stack, Cluster cluster) throws CloudbreakException {
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
        if (orchestratorType.containerOrchestrator() && cluster.getContainers().isEmpty()) {
            flowManager.triggerClusterInstall(stack.getId());
        } else {
            flowManager.triggerClusterReInstall(stack.getId());
        }
    }

    private boolean validateRequest(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        HostGroup hostGroup = getHostGroup(stack, hostGroupAdjustment);
        int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        boolean downScale = scalingAdjustment < 0;
        if (scalingAdjustment == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
        if (!downScale && hostGroup.getInstanceGroup() != null) {
            validateUnusedHosts(hostGroup.getInstanceGroup(), scalingAdjustment);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment);
            if (hostGroupAdjustment.getWithStackUpdate() && hostGroupAdjustment.getScalingAdjustment() > 0) {
                throw new BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.");
            }
        }
        return downScale;
    }

    private void validateComponentsCategory(Stack stack, String hostGroup) {
        Blueprint blueprint = stack.getCluster().getBlueprint();
        String blueprintText = blueprint.getBlueprintText();
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            String blueprintName = root.path("Blueprints").path("blueprint_name").asText();
            Map<String, String> categories =
                    clusterApiConnectors.getConnector(stack).clusterModificationService().getComponentsByCategory(blueprintName, hostGroup);
            for (Entry<String, String> entry : categories.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(MASTER_CATEGORY) && !blueprintUtils.isSharedServiceReadyBlueprint(blueprint)) {
                    throw new BadRequestException(
                            String.format("Cannot downscale the '%s' hostGroupAdjustment group, because it contains a '%s' component", hostGroup,
                                    entry.getKey()));
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot check the host components category", e);
        }
    }

    private void validateUnusedHosts(InstanceGroup instanceGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetaDataService.findUnusedHostsInInstanceGroup(instanceGroup.getId());
        if (unusedHostsInInstanceGroup.size() < scalingAdjustment) {
            throw new BadRequestException(String.format(
                    "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                    unusedHostsInInstanceGroup.size(), instanceGroup.getGroupName(), scalingAdjustment - unusedHostsInInstanceGroup.size()));
        }
    }

    private void validateRegisteredHosts(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String hostGroupName = hostGroupAdjustment.getHostGroup();
        hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName).ifPresentOrElse(hostGroup -> {
            if (hostGroup.getInstanceGroup() == null) {
                throw new BadRequestException(String.format("Can't find instancegroup for hostgroup: %s", hostGroupName));
            } else {
                InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
                int hostsCount = instanceGroup.getNotDeletedInstanceMetaDataSet().size();
                int adjustment = Math.abs(hostGroupAdjustment.getScalingAdjustment());
                Boolean validateNodeCount = hostGroupAdjustment.getValidateNodeCount();
                if (validateNodeCount == null || validateNodeCount) {
                    if (hostsCount <= adjustment) {
                        String errorMessage = String.format("[hostGroup: '%s', current hosts: %s, decommissions requested: %s]",
                                hostGroupName, hostsCount, adjustment);
                        throw new BadRequestException(String.format("The host group must contain at least 1 host after the decommission: %s", errorMessage));
                    }
                } else if (hostsCount - adjustment < 0) {
                    throw new BadRequestException(String.format("There are not enough hosts in host group: %s to remove", hostGroupName));
                }
            }
        }, () -> {
            throw new BadRequestException(String.format("Can't find hostgroup: %s", hostGroupName));
        });
    }

    private HostGroup getHostGroup(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupAdjustment.getHostGroup());
        if (hostGroup.isEmpty()) {
            throw new BadRequestException(String.format(
                    "Invalid host group: cluster '%s' does not contain a host group named '%s'.",
                    stack.getCluster().getName(), hostGroupAdjustment.getHostGroup()));
        }
        return hostGroup.get();
    }

    public Cluster getById(Long id) {
        return repository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("Cluster '%s' not found", id)));
    }

    public Map<HostName, String> getHostStatuses(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return clusterApiConnectors.getConnector(stack).clusterStatusService().getHostStatusesRaw();
    }

    public Set<Cluster> findByBlueprint(Blueprint blueprint) {
        return repository.findByBlueprint(blueprint);
    }

    public List<Cluster> findByStatuses(Collection<Status> statuses) {
        return repository.findByStatuses(statuses);
    }

    public Optional<Cluster> findOneByStackId(Long stackId) {
        return repository.findOneByStackId(stackId);
    }

    public Optional<Cluster> findOneWithLists(Long id) {
        return repository.findOneWithLists(id);
    }

    public Optional<Cluster> findById(Long clusterId) {
        return repository.findById(clusterId);
    }

    public Set<Cluster> findByRdsConfig(Long rdsConfigId) {
        return repository.findByRdsConfig(rdsConfigId);
    }

    public Set<String> findNamesByRdsConfig(Long rdsConfigId) {
        return repository.findNamesByRdsConfig(rdsConfigId);
    }

    public void triggerMaintenanceModeValidation(Stack stack) {
        flowManager.triggerMaintenanceModeValidationFlow(stack.getId());
    }

    public void pureDelete(Cluster cluster) {
        repository.delete(cluster);
    }

}