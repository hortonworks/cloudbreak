package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.MPACK_TAG;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPO_ID_TAG;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.constraint.ConstraintService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);

    private static final String MASTER_CATEGORY = "MASTER";

    private static final List<String> REATTACH_NOT_SUPPORTED_VOLUME_TYPES = List.of("ephemeral");

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
    private KerberosConfigService kerberosConfigService;

    @Inject
    private ConstraintService constraintService;

    @Inject
    private HostMetadataService hostMetadataService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private GatewayConvertUtil gateWayUtil;

    @Inject
    private ConverterUtil converterUtil;

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
    private RdsConfigService rdsConfigService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Measure(ClusterService.class)
    public Cluster create(Stack stack, Cluster cluster, List<ClusterComponent> components, User user) throws TransactionExecutionException {
        LOGGER.debug("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        String stackName = stack.getName();
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster().getName()));
        }
        return transactionService.required(() -> {
            setWorkspace(cluster, stack.getWorkspace());
            cluster.setEnvironment(stack.getEnvironment());

            long start = System.currentTimeMillis();
            if (repository.findByNameAndWorkspace(cluster.getName(), stack.getWorkspace()).isPresent()) {
                throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName());
            }
            LOGGER.debug("Cluster name collision check took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            if (Status.CREATE_FAILED.equals(stack.getStatus())) {
                throw new BadRequestException("Stack creation failed, cannot create cluster.");
            }

            start = System.currentTimeMillis();
            saveAllHostGroupContraint(cluster);
            LOGGER.debug("Host group constrainst saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            start = System.currentTimeMillis();
            if (cluster.getFileSystem() != null) {
                cluster.setFileSystem(fileSystemConfigService.create(cluster.getFileSystem(), cluster.getWorkspace(), user));
            }
            LOGGER.debug("Filesystem config saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            if (cluster.getKerberosConfig() != null) {
                kerberosConfigService.save(cluster.getKerberosConfig());
            }

            removeGatewayIfNotSupported(cluster, components);

            cluster.setStack(stack);
            stack.setCluster(cluster);

            start = System.currentTimeMillis();
            gateWayUtil.generateSignKeys(cluster.getGateway());
            LOGGER.debug("Sign key generated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

            Cluster savedCluster = saveClusterAndComponent(cluster, components, stackName);
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

    public Long countAliveByEnvironment(Environment environment) {
        return repository.countAliveOnesByWorkspaceAndEnvironment(environment.getWorkspace().getId(), environment.getId());
    }

    private void setWorkspace(Cluster cluster, Workspace workspace) {
        cluster.setWorkspace(workspace);
        if (cluster.getGateway() != null) {
            cluster.getGateway().setWorkspace(workspace);
        }
        if (cluster.getKerberosConfig() != null) {
            cluster.getKerberosConfig().setWorkspace(workspace);
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

    private void saveAllHostGroupContraint(Cluster cluster) {
        cluster.getHostGroups().stream()
                .map(HostGroup::getConstraint)
                .filter(Objects::nonNull)
                .forEach(it -> constraintService.save(it));
    }

    public boolean isMultipleGateway(Stack stack) {
        int gatewayCount = 0;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            if (ig.getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                gatewayCount += ig.getNodeCount();
            }
        }
        return gatewayCount > 1;
    }

    public boolean isSingleNode(Stack stack) {
        int nodeCount = 0;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            nodeCount += ig.getNodeCount();
        }
        return nodeCount == 1;
    }

    public void removeTerminatedPrimaryGateway(Long clusterId, String hostName, boolean singlePrimaryGateway) {
        if (!singlePrimaryGateway) {
            return;
        }

        Set<HostMetadata> primaryGateways = hostMetadataService.findHostsInClusterByName(clusterId, hostName);

        List<HostMetadata> oldPrimaryGateways = primaryGateways.stream()
                .filter(hmd -> hmd.getHostMetadataState() != HostMetadataState.SERVICES_RUNNING)
                .collect(Collectors.toList());
        hostMetadataService.deleteAll(oldPrimaryGateways);
    }

    public Iterable<Cluster> saveAll(Iterable<Cluster> clusters) {
        return repository.saveAll(clusters);
    }

    public Cluster save(Cluster cluster) {
        return repository.save(cluster);
    }

    public void delete(Long stackId, Boolean withStackDelete, Boolean deleteDependencies) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (stack.getCluster() == null || stack.getCluster() != null && Status.DELETE_COMPLETED.equals(stack.getCluster().getStatus())) {
            throw new BadRequestException("Clusters is already deleted.");
        }
        LOGGER.debug("Cluster delete requested.");
        markVolumesForDeletion(stack);
        flowManager.triggerClusterTermination(stackId, withStackDelete, deleteDependencies);
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

    public ClusterV4Response retrieveClusterForCurrentUser(Long stackId) {
        Stack stack = stackService.getById(stackId);
        return converterUtil.convert(stack.getCluster(), ClusterV4Response.class);
    }

    public Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig) {
        Cluster cluster = getCluster(clusterId);
        cluster.setAmbariIp(ambariClientConfig.getApiAddress());
        cluster = repository.save(cluster);
        LOGGER.debug("Updated cluster: [ambariIp: '{}'].", ambariClientConfig.getApiAddress());
        return cluster;
    }

    public void updateHostMetadata(Long clusterId, Map<String, List<String>> hostsPerHostGroup, HostMetadataState hostMetadataState) {
        try {
            transactionService.required(() -> {
                for (Entry<String, List<String>> hostGroupEntry : hostsPerHostGroup.entrySet()) {
                    Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(clusterId, hostGroupEntry.getKey());
                    if (hostGroup.isPresent()) {
                        Set<String> existingHosts = hostMetadataService.findEmptyHostsInHostGroup(hostGroup.get().getId()).stream()
                                .map(HostMetadata::getHostName)
                                .collect(Collectors.toSet());
                        hostGroupEntry.getValue().stream()
                                .filter(hostName -> !existingHosts.contains(hostName))
                                .forEach(hostName -> {
                                    HostMetadata hostMetadataEntry = new HostMetadata();
                                    hostMetadataEntry.setHostName(hostName);
                                    hostMetadataEntry.setHostGroup(hostGroup.get());
                                    hostMetadataEntry.setHostMetadataState(hostMetadataState);
                                    hostGroup.get().getHostMetadata().add(hostMetadataEntry);
                                });
                        hostGroupService.save(hostGroup.get());
                    }
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

    public void failureReport(Long stackId, List<String> failedNodes) {
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getById(stackId);
                Cluster cluster = stack.getCluster();
                Map<String, List<String>> autoRecoveryNodesMap = new HashMap<>();
                Map<String, HostMetadata> autoRecoveryHostMetadata = new HashMap<>();
                Map<String, HostMetadata> failedHostMetadata = new HashMap<>();
                for (String failedNode : failedNodes) {
                    Optional<HostMetadata> hostMetadata = hostMetadataService.findHostInClusterByName(cluster.getId(), failedNode);
                    if (hostMetadata.isEmpty()) {
                        throw new BadRequestException("No metadata information for the node: " + failedNode);
                    }
                    HostGroup hostGroup = hostMetadata.get().getHostGroup();
                    if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                        validateRepair(stack, hostMetadata.get(), false);
                    }
                    String hostGroupName = hostGroup.getName();
                    if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                        prepareForAutoRecovery(stack, autoRecoveryNodesMap, autoRecoveryHostMetadata, failedNode, hostMetadata.get(), hostGroupName);
                    } else if (hostGroup.getRecoveryMode() == RecoveryMode.MANUAL) {
                        failedHostMetadata.put(failedNode, hostMetadata.get());
                    }
                }
                try {
                    if (!autoRecoveryNodesMap.isEmpty()) {
                        flowManager.triggerClusterRepairFlow(stackId, autoRecoveryNodesMap, false);
                        String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.CLUSTER_AUTORECOVERY_REQUESTED.code(),
                                Collections.singletonList(autoRecoveryNodesMap));
                        updateChangedHosts(cluster, autoRecoveryHostMetadata, HostMetadataState.HEALTHY, HostMetadataState.WAITING_FOR_REPAIR, recoveryMessage);
                    }
                    if (!failedHostMetadata.isEmpty()) {
                        String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.CLUSTER_FAILED_NODES_REPORTED.code(),
                                Collections.singletonList(failedHostMetadata.keySet()));
                        updateChangedHosts(cluster, failedHostMetadata, HostMetadataState.HEALTHY, HostMetadataState.UNHEALTHY, recoveryMessage);
                    }
                } catch (TransactionExecutionException e) {
                    throw new TransactionRuntimeExecutionException(e);
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void prepareForAutoRecovery(Stack stack,
            Map<String, List<String>> autoRecoveryNodesMap,
            Map<String, HostMetadata> autoRecoveryHostMetadata,
            String failedNode,
            HostMetadata hostMetadata,
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

    public void repairCluster(Long stackId, List<String> repairedHostGroups, boolean removeOnly) {
        repairClusterInternal(true, stackId, repairedHostGroups, null, false, removeOnly);
    }

    public void repairCluster(Long stackId, List<String> nodeIds, boolean deleteVolumes, boolean removeOnly) {
        repairClusterInternal(false, stackId, null, nodeIds, deleteVolumes, removeOnly);
    }

    private void repairClusterInternal(boolean hostGroupMode, Long stackId, List<String> repairedHostGroups,
            List<String> nodeIds, boolean deleteVolumes, boolean removeOnly) {
        Map<String, List<String>> hostGroupToNodesMap = new HashMap<>();
        try {
            transactionService.required(() -> {
                Stack inTransactionStack = stackService.get(stackId);
                boolean repairWithReattach = !deleteVolumes;
                checkReattachSupportedOnProvider(inTransactionStack, repairWithReattach);
                Cluster cluster = inTransactionStack.getCluster();
                Set<String> instanceHostNames = getInstanceHostNames(hostGroupMode, inTransactionStack, nodeIds);
                Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
                for (HostGroup hg : hostGroups) {
                    List<String> nodesToRepair = new ArrayList<>();
                    if (hg.getRecoveryMode() == RecoveryMode.MANUAL && (!hostGroupMode || repairedHostGroups.contains(hg.getName()))) {
                        for (HostMetadata hmd : hg.getHostMetadata()) {
                            checkReattachSupportForGateways(inTransactionStack, repairWithReattach, cluster, hmd);
                            if (isRepairNeededForHost(hostGroupMode, instanceHostNames, hmd)) {
                                checkDiskTypeSupported(inTransactionStack, repairWithReattach, hg);
                                validateRepair(inTransactionStack, hmd, repairWithReattach);
                                hostGroupToNodesMap.putIfAbsent(hg.getName(), nodesToRepair);
                                nodesToRepair.add(hmd.getHostName());
                            }
                        }
                    }
                }
                if (!hostGroupMode) {
                    updateNodeVolumeSetsDeleteVolumesFlag(inTransactionStack, nodeIds, deleteVolumes);
                }
                return inTransactionStack;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        List<String> repairedEntities = CollectionUtils.isEmpty(repairedHostGroups) ? nodeIds : repairedHostGroups;
        triggerRepair(stackId, hostGroupToNodesMap, removeOnly, repairedEntities);
    }

    private void checkReattachSupportForGateways(Stack inTransactionStack, boolean repairWithReattach, Cluster cluster, HostMetadata hmd) {
        boolean ambariRdsPresent = cluster.getRdsConfigs().stream()
                .anyMatch(rdsConfig -> "AMBARI".equals(rdsConfig.getType()) && ResourceStatus.USER_MANAGED.equals(rdsConfig.getStatus()));
        boolean singleNodeGateway = isGateway(hmd) && !isMultipleGateway(inTransactionStack);
        if (repairWithReattach && !ambariRdsPresent && singleNodeGateway) {
            throw new BadRequestException("Repair with disk reattach not supported on single node gateway without external Ambari RDS.");
        }
    }

    private void checkReattachSupportedOnProvider(Stack inTransactionStack, boolean repairWithReattach) {
        if (repairWithReattach && !StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(inTransactionStack.getPlatformVariant())) {
            throw new BadRequestException("Volume reattach currently not supported!");
        }
    }

    private void checkDiskTypeSupported(Stack inTransactionStack, boolean repairWithReattach, HostGroup hg) {
        if (repairWithReattach
                && inTransactionStack.getInstanceGroupsAsList().stream()
                .filter(instanceGroup -> hg.getName().equalsIgnoreCase(instanceGroup.getGroupName()))
                .map(InstanceGroup::getTemplate)
                .map(Template::getVolumeTemplates)
                .anyMatch(volumes -> volumes.stream()
                        .map(VolumeTemplate::getVolumeType).anyMatch(REATTACH_NOT_SUPPORTED_VOLUME_TYPES::contains))) {
            throw new BadRequestException("Reattach not supported for this disk type.");
        }
    }

    private boolean isRepairNeededForHost(boolean hostGroupMode, Set<String> instanceHostNames, HostMetadata hmd) {
        return hostGroupMode ? hmd.getHostMetadataState() == HostMetadataState.UNHEALTHY : instanceHostNames.contains(hmd.getHostName());
    }

    private Set<String> getInstanceHostNames(boolean hostGroupMode, Stack stack, List<String> nodeIds) {
        if (hostGroupMode) {
            return Set.of();
        }
        Set<String> instanceHostNames = stack.getInstanceMetaDataAsList()
                .stream()
                .filter(md -> nodeIds.contains(md.getInstanceId()))
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.toSet());
        validateRepairNodeIdRequest(nodeIds, instanceHostNames);
        return instanceHostNames;
    }

    private void validateRepairNodeIdRequest(List<String> nodeIds, Set<String> instanceHostNames) {
        long distinctNodeIdCount = nodeIds.stream().distinct().count();
        if (distinctNodeIdCount != instanceHostNames.size()) {
            throw new BadRequestException(String.format("Node ID list is not valid: [%s]", String.join(", ", nodeIds)));
        }
    }

    private void validateRepair(Stack stack, HostMetadata hostMetadata, boolean repairWithReattach) {
//        if (!repairWithReattach && (isGateway(hostMetadata) && !isMultipleGateway(stack))) {
//            throw new BadRequestException("Ambari server failure cannot be repaired with single gateway!");
//        }
        if (isGateway(hostMetadata) && withEmbeddedAmbariDB(stack.getCluster())) {
            throw new BadRequestException("Ambari server failure with embedded database cannot be repaired!");
        }
    }

    private void updateNodeVolumeSetsDeleteVolumesFlag(Stack stack, List<String> nodeIds, boolean deleteVolumes) {
        resourceService.saveAll(stack.getDiskResources().stream()
                .filter(resource -> nodeIds.contains(resource.getInstanceId()))
                .map(volumeSet -> updateDeleteVolumesFlag(deleteVolumes, volumeSet))
                .collect(Collectors.toList()));
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }

    private void triggerRepair(Long stackId, Map<String, List<String>> failedNodeMap, boolean removeOnly, List<String> recoveryMessageArgument) {
        if (!failedNodeMap.isEmpty()) {
            flowManager.triggerClusterRepairFlow(stackId, failedNodeMap, removeOnly);
            String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.CLUSTER_MANUALRECOVERY_REQUESTED.code(),
                    Collections.singletonList(recoveryMessageArgument));
            LOGGER.debug(recoveryMessage);
            eventService.fireCloudbreakEvent(stackId, "RECOVERY", recoveryMessage);
        } else {
            throw new BadRequestException(String.format("Could not trigger cluster repair  for stack %s, because node list is incorrect", stackId));
        }
    }

    private boolean isGateway(HostMetadata hostMetadata) {
        return hostMetadata.getHostGroup().getConstraint().getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY;
    }

    private boolean withEmbeddedAmbariDB(Cluster cluster) {
        RDSConfig rdsConfig = rdsConfigService.findByClusterIdAndType(cluster.getId(), DatabaseType.AMBARI);
        return rdsConfig == null || DatabaseVendor.EMBEDDED == rdsConfig.getDatabaseEngine();
    }

    private void updateChangedHosts(Cluster cluster, Map<String, HostMetadata> failedHostMetadata, HostMetadataState healthyState,
            HostMetadataState unhealthyState, String recoveryMessage) throws TransactionExecutionException {
        Set<HostMetadata> hosts = hostMetadataService.findHostsInCluster(cluster.getId());
        Collection<HostMetadata> changedHosts = new HashSet<>();
        transactionService.required(() -> {
            for (HostMetadata host : hosts) {
                if (host.getHostMetadataState() == unhealthyState && !failedHostMetadata.containsKey(host.getHostName())) {
                    host.setHostMetadataState(healthyState);
                    changedHosts.add(host);
                } else if (host.getHostMetadataState() == healthyState && failedHostMetadata.containsKey(host.getHostName())) {
                    host.setHostMetadataState(unhealthyState);
                    changedHosts.add(host);
                }
            }
            if (!changedHosts.isEmpty()) {
                LOGGER.debug(recoveryMessage);
                eventService.fireCloudbreakEvent(cluster.getStack().getId(), "RECOVERY", recoveryMessage);
                hostMetadataService.saveAll(changedHosts);
            }
            return null;
        });
    }

    private void sync(Stack stack) {
        flowManager.triggerClusterSync(stack.getId());
    }

    private void start(Stack stack, Cluster cluster) {
        if (stack.isStartInProgress()) {
            String message = cloudbreakMessagesService.getMessage(Msg.CLUSTER_START_REQUESTED.code());
            eventService.fireCloudbreakEvent(stack.getId(), START_REQUESTED.name(), message);
            updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        } else {
            if (cluster.isAvailable()) {
                String statusDesc = cloudbreakMessagesService.getMessage(Msg.CLUSTER_START_IGNORED.code());
                LOGGER.debug(statusDesc);
                eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), statusDesc);
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
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.CLUSTER_STOP_IGNORED.code());
            LOGGER.debug(statusDesc);
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), statusDesc);
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
        if (cluster.isPresent()) {
            cluster.get().setStatus(status);
            cluster.get().setStatusReason(statusReason);
            cluster = Optional.ofNullable(repository.save(cluster.get()));
            if (status.isRemovableStatus()) {
                InMemoryStateStore.deleteCluster(cluster.get().getId());
                if (stackStatus.getStatus().isRemovableStatus()) {
                    InMemoryStateStore.deleteStack(stackId);
                }
            } else {
                InMemoryStateStore.putCluster(cluster.get().getId(), statusToPollGroupConverter.convert(status));
                if (InMemoryStateStore.getStack(stackId) == null) {
                    InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(stackStatus.getStatus()));
                }
            }
        }
        return cluster.orElse(null);
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
        Map<String, HostMetadataState> hostStatuses = connector.clusterStatusService().getHostStatuses();
        Set<HostMetadata> hosts = hostMetadataService.findHostsInCluster(stack.getCluster().getId());
        try {
            return transactionService.required(() -> {
                for (HostMetadata host : hosts) {
                    if (hostStatuses.containsKey(host.getHostName())) {
                        HostMetadataState newState = hostStatuses.get(host.getHostName());
                        boolean stateChanged = updateHostMetadataByHostState(stack, host.getHostName(), newState);
                        if (stateChanged && HostMetadataState.HEALTHY == newState) {
                            updateInstanceMetadataStateToRegistered(stackId, host);
                        }
                    }
                }
                return stack.getCluster();
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void updateInstanceMetadataStateToRegistered(Long stackId, HostMetadata host) {
        Optional<InstanceMetaData> instanceMetaData = instanceMetaDataService.findHostInStack(stackId, host.getHostName());
        if (instanceMetaData.isPresent()) {
            instanceMetaData.get().setInstanceStatus(InstanceStatus.REGISTERED);
            instanceMetaDataService.save(instanceMetaData.get());
        }
    }

    public Cluster recreate(Stack stack, String blueprintName, Set<HostGroup> hostGroups, boolean validateBlueprint,
            StackRepoDetails stackRepoDetails, String kerberosPassword, String kerberosPrincipal) throws TransactionExecutionException {
        return transactionService.required(() -> {
            checkBlueprintIdAndHostGroups(blueprintName, hostGroups);
            Stack stackWithLists = stackService.getByIdWithListsInTransaction(stack.getId());
            Cluster cluster = getCluster(stackWithLists);
            if (cluster != null && stackWithLists.getCluster().getKerberosConfig() != null) {
                initKerberos(kerberosPassword, kerberosPrincipal, cluster);
            }
            Blueprint blueprint = blueprintService.getByNameForWorkspace(blueprintName, stack.getWorkspace());
            if (!withEmbeddedAmbariDB(cluster)) {
                throw new BadRequestException("Ambari doesn't support resetting external DB automatically. To reset Ambari Server schema you must first drop "
                        + "and then create it using DDL scripts from /var/lib/ambari-server/resources");
            }
            if (validateBlueprint) {
                ambariBlueprintValidator.validateBlueprintForStack(cluster, blueprint, hostGroups, stackWithLists.getInstanceGroups());
            }
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
                cluster = prepareCluster(hostGroups, stackRepoDetails, blueprint, stackWithLists, cluster);
                triggerClusterInstall(stackWithLists, cluster);
            } catch (CloudbreakException e) {
                throw new CloudbreakServiceException(e);
            }
            return stackWithLists.getCluster();
        });
    }

    private void initKerberos(String kerberosPassword, String kerberosPrincipal, Cluster cluster) {
        List<String> missing = Stream.of(Pair.of("password", kerberosPassword), Pair.of("principal", kerberosPrincipal))
                .filter(p -> !StringUtils.hasLength(p.getRight()))
                .map(Pair::getLeft).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new BadRequestException(String.format("Missing Kerberos credential detail(s): %s", String.join(", ", missing)));
        }
        KerberosConfig kerberosConfig = cluster.getKerberosConfig();
        kerberosConfig.setPassword(kerberosPassword);
        kerberosConfig.setPrincipal(kerberosPrincipal);

        kerberosConfigService.save(kerberosConfig);
    }

    private void checkBlueprintIdAndHostGroups(String blueprint, Set<HostGroup> hostGroups) {
        if (blueprint == null || hostGroups == null) {
            throw new BadRequestException("Cluster definition id and hostGroup assignments can not be null.");
        }
    }

    private Cluster prepareCluster(Collection<HostGroup> hostGroups, StackRepoDetails stackRepoDetails, Blueprint blueprint, Stack stack,
            Cluster cluster) {
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().clear();
        cluster.getHostGroups().addAll(hostGroups);
        createHDPRepoComponent(stackRepoDetails, stack);
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

    public void upgrade(Long stackId, AmbariRepo ambariRepoUpgrade) throws TransactionExecutionException {
        if (ambariRepoUpgrade != null) {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Cluster cluster = getCluster(stack);
            if (cluster == null) {
                throw new BadRequestException(String.format("Cluster does not exist on stack with '%s' id.", stackId));
            }
            if (!stack.isAvailable()) {
                throw new BadRequestException(String.format(
                        "Stack '%s' is currently in '%s' state. Upgrade requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.",
                        stackId, stack.getStatus()));
            }
            if (!cluster.isAvailable()) {
                throw new BadRequestException(String.format(
                        "Cluster '%s' is currently in '%s' state. Upgrade requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.",
                        stackId, stack.getStatus()));
            }
            AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(cluster.getId());
            transactionService.required(() -> {
                if (ambariRepo == null) {
                    try {
                        clusterComponentConfigProvider.store(new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS,
                                new Json(ambariRepoUpgrade), stack.getCluster()));
                    } catch (IllegalArgumentException ignored) {
                        throw new BadRequestException(String.format("Ambari repo details cannot be saved. %s", ambariRepoUpgrade));
                    }
                } else {
                    ClusterComponent component = clusterComponentConfigProvider.getComponent(cluster.getId(), ComponentType.AMBARI_REPO_DETAILS);
                    ambariRepo.setBaseUrl(ambariRepoUpgrade.getBaseUrl());
                    ambariRepo.setGpgKeyUrl(ambariRepoUpgrade.getGpgKeyUrl());
                    ambariRepo.setPredefined(false);
                    ambariRepo.setVersion(ambariRepoUpgrade.getVersion());
                    try {
                        component.setAttributes(new Json(ambariRepo));
                        clusterComponentConfigProvider.store(component);
                    } catch (IllegalArgumentException ignored) {
                        throw new BadRequestException(String.format("Ambari repo details cannot be saved. %s", ambariRepoUpgrade));
                    }
                }
                try {
                    flowManager.triggerClusterUpgrade(stack.getId());
                } catch (RuntimeException e) {
                    throw new CloudbreakServiceException(e);
                }
                return null;
            });
        }
    }

    private void createHDPRepoComponent(StackRepoDetails stackRepoDetailsUpdate, Stack stack) {
        if (stackRepoDetailsUpdate != null) {
            StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(stack.getCluster().getId());
            if (stackRepoDetails == null) {
                try {
                    ClusterComponent clusterComp = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, new Json(stackRepoDetailsUpdate), stack.getCluster());
                    clusterComponentConfigProvider.store(clusterComp);
                } catch (IllegalArgumentException ignored) {
                    throw new BadRequestException(String.format("HDP Repo parameters cannot be converted. %s", stackRepoDetailsUpdate));
                }
            } else {
                ClusterComponent component = clusterComponentConfigProvider.getComponent(stack.getCluster().getId(), ComponentType.HDP_REPO_DETAILS);
                stackRepoDetails.setHdpVersion(stackRepoDetailsUpdate.getHdpVersion());
                stackRepoDetails.setVerify(stackRepoDetailsUpdate.isVerify());
                stackRepoDetails.setStack(stackRepoDetailsUpdate.getStack());
                stackRepoDetails.setUtil(stackRepoDetailsUpdate.getUtil());
                stackRepoDetails.setEnableGplRepo(stackRepoDetailsUpdate.isEnableGplRepo());
                stackRepoDetails.setMpacks(stackRepoDetailsUpdate.getMpacks());
                try {
                    component.setAttributes(new Json(stackRepoDetails));
                    clusterComponentConfigProvider.store(component);
                } catch (IllegalArgumentException ignored) {
                    throw new BadRequestException(String.format("HDP Repo parameters cannot be converted. %s", stackRepoDetailsUpdate));
                }
            }
        }
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
        Blueprint clusterDefinition = stack.getCluster().getBlueprint();
        if (blueprintService.isAmbariBlueprint(clusterDefinition)) {
            ambariBlueprintValidator.validateHostGroupScalingRequest(stack.getCluster().getBlueprint(), hostGroup, scalingAdjustment);
        }
        if (!downScale && hostGroup.getConstraint().getInstanceGroup() != null) {
            validateUnusedHosts(hostGroup.getConstraint().getInstanceGroup(), scalingAdjustment);
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
        String hostGroup = hostGroupAdjustment.getHostGroup();
        int hostsCount = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroup)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroup)).getHostMetadata().size();
        int adjustment = Math.abs(hostGroupAdjustment.getScalingAdjustment());
        Boolean validateNodeCount = hostGroupAdjustment.getValidateNodeCount();
        if (validateNodeCount == null || validateNodeCount) {
            if (hostsCount <= adjustment) {
                String errorMessage = String.format("[hostGroup: '%s', current hosts: %s, decommissions requested: %s]", hostGroup, hostsCount, adjustment);
                throw new BadRequestException(String.format("The host group must contain at least 1 host after the decommission: %s", errorMessage));
            }
        } else if (hostsCount - adjustment < 0) {
            throw new BadRequestException(String.format("There are not enough hosts in host group: %s to remove", hostGroup));
        }
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

    private boolean updateHostMetadataByHostState(Stack stack, String hostName, HostMetadataState newState) {
        boolean stateChanged = false;
        HostMetadata hostMetadata = hostMetadataService.findHostInClusterByName(stack.getCluster().getId(), hostName)
                .orElseThrow(NotFoundException.notFound("hostmetadata", hostName));
        HostMetadataState oldState = hostMetadata.getHostMetadataState();
        if (!oldState.equals(newState)) {
            stateChanged = true;
            hostMetadata.setHostMetadataState(newState);
            hostMetadataService.save(hostMetadata);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.CLUSTER_HOST_STATUS_UPDATED.code(), Arrays.asList(hostName, newState.name())));
        }
        return stateChanged;
    }

    public Cluster getById(Long id) {
        return repository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("Cluster '%s' not found", id)));
    }

    public Map<String, String> getHostStatuses(Long stackId) {
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

    public List<Cluster> findAllClustersForConstraintTemplate(Long constraintTemplateId) {
        return repository.findAllClustersForConstraintTemplate(constraintTemplateId);
    }

    public Set<Cluster> findByLdapConfig(LdapConfig ldapConfig) {
        return repository.findByLdapConfigAndStatusNot(ldapConfig, Status.DELETE_COMPLETED);
    }

    public Set<Cluster> findAllClustersByLdapConfigInEnvironment(LdapConfig ldapConfig, Long environmentId) {
        return repository.findByLdapConfigAndEnvironment(ldapConfig, environmentId);
    }

    public Set<Cluster> findByProxyConfig(ProxyConfig proxyConfig) {
        return repository.findByProxyConfigAndStatusNot(proxyConfig, Status.DELETE_COMPLETED);
    }

    public Set<Cluster> findAllClustersByProxyConfigInEnvironment(ProxyConfig proxyConfig, Long environmentId) {
        return repository.findByProxyConfigAndEnvironment(proxyConfig, environmentId);
    }

    public Set<Cluster> findByRdsConfig(Long rdsConfigId) {
        return repository.findByRdsConfig(rdsConfigId);
    }

    public Set<Cluster> findAllClustersByRdsConfigInEnvironment(RDSConfig rdsConfig, Long environmentId) {
        return repository.findByRdsConfigAndEnvironment(rdsConfig.getId(), environmentId);
    }

    public Set<Cluster> findByKerberosConfig(Long kerberosConfigId) {
        return repository.findByKerberosConfig(kerberosConfigId);
    }

    public Set<Cluster> findAllClustersByKerberosConfigInEnvironment(KerberosConfig kerberosConfig, Long environmentId) {
        return repository.findByKerberosConfigAndEnvironment(kerberosConfig.getId(), environmentId);
    }

    public void updateAmbariRepoDetails(Long clusterId, StackRepositoryV4Request stackRepository) {
        if (Objects.isNull(stackRepository.getVersion()) || Objects.isNull(stackRepository.getRepository().getBaseUrl())) {
            throw new BadRequestException("Ambari repo details not complete.");
        }

        AmbariRepo ambariRepo = clusterComponentConfigProvider.getAmbariRepo(clusterId);
        ambariRepo.setVersion(stackRepository.getVersion());
        ambariRepo.setBaseUrl(stackRepository.getRepository().getBaseUrl());
        ambariRepo.setPredefined(Boolean.FALSE);
        Optional.ofNullable(stackRepository.getRepository().getGpgKeyUrl()).ifPresent(ambariRepo::setGpgKeyUrl);

        ClusterComponent component = clusterComponentConfigProvider.getComponent(clusterId, ComponentType.AMBARI_REPO_DETAILS);

        try {
            component.setAttributes(new Json(ambariRepo));
            clusterComponentConfigProvider.store(component);
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestException("Ambari repo details cannot be saved.");
        }
    }

    public void updateHdpRepoDetails(Long clusterId, StackRepositoryV4Request stackRepository) {
        checkMandatoryHdpFields(stackRepository);

        StackRepoDetails hdpRepo = clusterComponentConfigProvider.getStackRepoDetails(clusterId);

        Map<String, String> stack = Optional.ofNullable(hdpRepo.getStack()).orElseGet(HashMap::new);
        stack.put(REPO_ID_TAG, stackRepository.getRepoId());
        stack.put(stackRepository.getOsType(), stackRepository.getRepository().getBaseUrl());
        stack.put(REPOSITORY_VERSION, stackRepository.getRepository().getVersion());
        stack.put(VDF_REPO_KEY_PREFIX + stackRepository.getOsType(), stackRepository.getVersionDefinitionFileUrl());
        stack.put(CUSTOM_VDF_REPO_KEY, stackRepository.getVersionDefinitionFileUrl());
        hdpRepo.setStack(stack);

        Map<String, String> util = Optional.ofNullable(hdpRepo.getUtil()).orElseGet(HashMap::new);
        util.put(REPO_ID_TAG, stackRepository.getUtilsRepoId());
        util.put(stackRepository.getOsType(), stackRepository.getUtilsBaseURL());
        hdpRepo.setUtil(util);

        hdpRepo.setEnableGplRepo(stackRepository.isEnableGplRepo());
        Optional.ofNullable(stackRepository.getVerify()).or(() -> Optional.of(Boolean.TRUE)).ifPresent(hdpRepo::setVerify);
        hdpRepo.setHdpVersion(stackRepository.getVersion());

        hdpRepo.setMpacks(List.of());

        ClusterComponent component = clusterComponentConfigProvider.getComponent(clusterId, ComponentType.HDP_REPO_DETAILS);
        try {
            component.setAttributes(new Json(hdpRepo));
            clusterComponentConfigProvider.store(component);
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestException("HDP repo details cannot be saved.");
        }
    }

    private void checkMandatoryHdpFields(StackRepositoryV4Request stackRepository) {
        if (isAnyHdpHdfCommonFieldNull(stackRepository)) {
            throw new BadRequestException("HDP repo details not complete.");
        }
    }

    private boolean isAnyHdpHdfCommonFieldNull(StackRepositoryV4Request stackRepository) {
        return Objects.isNull(stackRepository.getRepoId())
                || Objects.isNull(stackRepository.getOsType())
                || Objects.isNull(stackRepository.getRepository().getBaseUrl())
                || Objects.isNull(stackRepository.getRepository().getVersion())
                || Objects.isNull(stackRepository.getVersionDefinitionFileUrl())
                || Objects.isNull(stackRepository.getUtilsRepoId())
                || Objects.isNull(stackRepository.getUtilsBaseURL())
                || Objects.isNull(stackRepository.isEnableGplRepo())
                || Objects.isNull(stackRepository.getVersion());
    }

    public void updateHdfRepoDetails(Long clusterId, StackRepositoryV4Request stackRepository) {
        if (isAnyHdpHdfCommonFieldNull(stackRepository)
                || Objects.isNull(stackRepository.getVersionDefinitionFileUrl())
                || Objects.isNull(stackRepository.getMpackUrl())) {
            throw new BadRequestException("HDF repo details not complete.");
        }

        StackRepoDetails hdfRepo = clusterComponentConfigProvider.getStackRepoDetails(clusterId);

        Map<String, String> stack = Optional.ofNullable(hdfRepo.getStack()).orElseGet(HashMap::new);
        stack.put(REPO_ID_TAG, stackRepository.getRepoId());
        stack.put(stackRepository.getOsType(), stackRepository.getRepository().getBaseUrl());
        stack.put(REPOSITORY_VERSION, stackRepository.getRepository().getVersion());
        stack.put(VDF_REPO_KEY_PREFIX + stackRepository.getOsType(), stackRepository.getVersionDefinitionFileUrl());
        stack.put(CUSTOM_VDF_REPO_KEY, stackRepository.getVersionDefinitionFileUrl());
        stack.put(MPACK_TAG, stackRepository.getMpackUrl());
        hdfRepo.setStack(stack);

        Map<String, String> util = Optional.ofNullable(hdfRepo.getUtil()).orElseGet(HashMap::new);
        util.put(REPO_ID_TAG, stackRepository.getUtilsRepoId());
        util.put(stackRepository.getOsType(), stackRepository.getUtilsBaseURL());
        hdfRepo.setUtil(util);

        hdfRepo.setEnableGplRepo(stackRepository.isEnableGplRepo());
        Optional.ofNullable(stackRepository.getVerify()).or(() -> Optional.of(Boolean.TRUE)).ifPresent(hdfRepo::setVerify);
        hdfRepo.setHdpVersion(stackRepository.getVersion());

        ManagementPackComponent managementPackComponent = new ManagementPackComponent();
        ManagementPackDetailsV4Request managementPackDetails = stackRepository.getMpacks().get(0);
        managementPackComponent.setName(managementPackDetails.getName());
        managementPackComponent.setMpackUrl(stackRepository.getMpackUrl());
        managementPackComponent.setPurge(false);
        managementPackComponent.setPurgeList(List.of());
        managementPackComponent.setForce(false);
        managementPackComponent.setStackDefault(true);
        managementPackComponent.setPreInstalled(managementPackDetails.isPreInstalled());
        hdfRepo.setMpacks(List.of(managementPackComponent));

        ClusterComponent component = clusterComponentConfigProvider.getComponent(clusterId, ComponentType.HDP_REPO_DETAILS);
        try {
            component.setAttributes(new Json(hdfRepo));
            clusterComponentConfigProvider.store(component);
        } catch (IllegalArgumentException ignored) {
            throw new BadRequestException("HDF repo details cannot be saved.");
        }
    }

    private void checkMandatoryHdfFields(StackRepositoryV4Request stackRepository) {
        if (isAnyHdpHdfCommonFieldNull(stackRepository)
                || Objects.isNull(stackRepository.getMpackUrl())
                || (Objects.isNull(stackRepository.getMpacks()) && StringUtils.isEmpty(stackRepository.getMpacks().iterator().next().getName()))) {
            throw new BadRequestException("HDF repo details not complete.");
        }
    }

    public void triggerMaintenanceModeValidation(Stack stack) {
        flowManager.triggerMaintenanceModeValidationFlow(stack.getId());
    }

    public void pureDelete(Cluster cluster) {
        repository.delete(cluster);
    }

    private enum Msg {
        CLUSTER_START_IGNORED("cluster.start.ignored"),
        CLUSTER_STOP_IGNORED("cluster.stop.ignored"),
        CLUSTER_HOST_STATUS_UPDATED("cluster.host.status.updated"),
        CLUSTER_START_REQUESTED("cluster.start.requested"),
        CLUSTER_AUTORECOVERY_REQUESTED("cluster.autorecovery.requested"),
        CLUSTER_MANUALRECOVERY_REQUESTED("cluster.manualrecovery.requested"),
        CLUSTER_FAILED_NODES_REPORTED("cluster.failednodes.reported");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

}
