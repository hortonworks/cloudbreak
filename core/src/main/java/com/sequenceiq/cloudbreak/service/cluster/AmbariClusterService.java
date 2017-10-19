package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackStatus;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
@Transactional
public class AmbariClusterService implements ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterService.class);

    private static final String MASTER_CATEGORY = "MASTER";

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    @Transactional(TxType.NEVER)
    public Cluster create(IdentityUser user, Stack stack, Cluster cluster, List<ClusterComponent> components) {
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster().getName()));
        }
        if (clusterRepository.findByNameInAccount(cluster.getName(), user.getAccount()) != null) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName());
        }
        if (Status.CREATE_FAILED.equals(stack.getStatus())) {
            throw new BadRequestException("Stack creation failed, cannot create cluster.");
        }
        for (HostGroup hostGroup : cluster.getHostGroups()) {
            constraintRepository.save(hostGroup.getConstraint());
        }
        if (cluster.getFileSystem() != null) {
            fileSystemRepository.save(cluster.getFileSystem());
        }
        if (cluster.getKerberosConfig() != null) {
            kerberosConfigRepository.save(cluster.getKerberosConfig());
        }
        cluster.setStack(stack);
        cluster.setOwner(user.getUserId());
        cluster.setAccount(user.getAccount());
        stack.setCluster(cluster);
        generateSignKeys(cluster.getGateway());
        try {
            cluster = clusterRepository.save(cluster);
            clusterComponentConfigProvider.store(components, cluster);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName(), ex);
        }
        if (stack.isAvailable()) {
            flowManager.triggerClusterInstall(stack.getId());
            InMemoryStateStore.putCluster(cluster.getId(), statusToPollGroupConverter.convert(cluster.getStatus()));
            if (InMemoryStateStore.getStack(stack.getId()) == null) {
                InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
            }
        }
        return cluster;
    }

    private boolean isMultipleGateway(Stack stack) {
        int gatewayCount = 0;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            if (ig.getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                gatewayCount += ig.getNodeCount();
            }
        }
        return gatewayCount > 1;
    }

    private boolean isEmbeddedAmbariDB(List<ClusterComponent> components) {
        AmbariDatabase ambariDatabase = clusterComponentConfigProvider.getComponent(components, AmbariDatabase.class, ComponentType.AMBARI_DATABASE_DETAILS);
        return ambariDatabase == null || DatabaseVendor.EMBEDDED.value().equals(ambariDatabase.getVendor());
    }

    @Override
    public void delete(Long stackId) {
        Stack stack = stackService.get(stackId);
        authorizationService.hasWritePermission(stack);
        if (stack.getCluster() != null && Status.DELETE_COMPLETED.equals(stack.getCluster().getStatus())) {
            throw new BadRequestException("Clusters is already deleted.");
        }
        LOGGER.info("Cluster delete requested.");
        flowManager.triggerClusterTermination(stackId);
    }

    @Override
    public Cluster retrieveClusterByStackId(Long stackId) {
        return clusterRepository.findOneByStackId(stackId);
    }

    @Override
    public <R extends ClusterResponse> R retrieveClusterForCurrentUser(Long stackId, Class<R> clazz) {
        Stack stack = stackService.get(stackId);
        return conversionService.convert(stack.getCluster(), clazz);
    }

    @Override
    @Transactional(TxType.NEVER)
    public Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig) {
        Cluster cluster = clusterRepository.findById(clusterId);
        cluster.setAmbariIp(ambariClientConfig.getApiAddress());
        cluster = clusterRepository.save(cluster);
        LOGGER.info("Updated cluster: [ambariIp: '{}'].", ambariClientConfig.getApiAddress());
        return cluster;
    }

    @Override
    public void updateHostCountWithAdjustment(Long clusterId, String hostGroupName, Integer adjustment) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterId, hostGroupName);
        Constraint constraint = hostGroup.getConstraint();
        constraint.setHostCount(constraint.getHostCount() + adjustment);
        constraintRepository.save(constraint);
    }

    @Override
    public void updateHostMetadata(Long clusterId, Map<String, List<String>> hostsPerHostGroup, HostMetadataState hostMetadataState) {
        for (Entry<String, List<String>> hostGroupEntry : hostsPerHostGroup.entrySet()) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterId, hostGroupEntry.getKey());
            if (hostGroup != null) {
                Set<String> existingHosts = hostMetadataRepository.findEmptyHostsInHostGroup(hostGroup.getId()).stream()
                    .map(HostMetadata::getHostName)
                    .collect(Collectors.toSet());
                hostGroupEntry.getValue().stream()
                    .filter(hostName -> !existingHosts.contains(hostName))
                    .forEach(hostName -> {
                        HostMetadata hostMetadataEntry = new HostMetadata();
                        hostMetadataEntry.setHostName(hostName);
                        hostMetadataEntry.setHostGroup(hostGroup);
                        hostMetadataEntry.setHostMetadataState(hostMetadataState);
                        hostGroup.getHostMetadata().add(hostMetadataEntry);
                    });
                hostGroupService.save(hostGroup);
            }
        }
    }

    @Override
    public String getClusterJson(String ambariIp, Long stackId) {
        Stack stack = stackService.get(stackId);
        if (stack.getAmbariIp() == null) {
            throw new NotFoundException(String.format("Ambari server is not available for the stack.[id: %s]", stackId));
        }
        Cluster cluster = stack.getCluster();
        try {
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stackId, cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
            String clusterJson = ambariClient.getClusterAsJson();
            if (clusterJson == null) {
                throw new BadRequestException(String.format("Cluster response coming from Ambari server was null. [Ambari Server IP: '%s']", ambariIp));
            }
            return clusterJson;
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Could not get Cluster from Ambari as JSON: " + errorMessage, e);
            }
        } catch (CloudbreakSecuritySetupException se) {
            throw new CloudbreakServiceException(se);
        }
    }

    @Override
    public void updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        boolean downscaleRequest = validateRequest(stack, hostGroupAdjustment);
        if (downscaleRequest) {
            updateClusterStatusByStackId(stackId, UPDATE_REQUESTED);
            flowManager.triggerClusterDownscale(stackId, hostGroupAdjustment);
        } else {
            flowManager.triggerClusterUpscale(stackId, hostGroupAdjustment);
        }
    }

    @Override
    @Transactional(TxType.NEVER)
    public void updateStatus(Long stackId, StatusRequest statusRequest) {
        Stack stack = stackService.getByIdWithLists(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
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

    @Override
    public void updateUserNamePassword(Long stackId, UserNamePasswordJson userNamePasswordJson) {
        Stack stack = stackService.get(stackId);
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

    @Override
    public void failureReport(Long stackId, List<String> failedNodes) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        Map<String, List<String>> autoRecoveryNodesMap = new HashMap<>();
        Map<String, HostMetadata> autoRecoveryHostMetadata = new HashMap<>();
        Map<String, HostMetadata> failedHostMetadata = new HashMap<>();
        for (String failedNode : failedNodes) {
            HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(cluster.getId(), failedNode);
            if (hostMetadata == null) {
                throw new BadRequestException("No metadata information for the node: " + failedNode);
            }
            HostGroup hostGroup = hostMetadata.getHostGroup();
            if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                validateRepair(stack, hostMetadata);
            }
            String hostGroupName = hostGroup.getName();
            if (hostGroup.getRecoveryMode() == RecoveryMode.AUTO) {
                List<String> nodeList = autoRecoveryNodesMap.get(hostGroupName);
                if (nodeList == null) {
                    validateComponentsCategory(stack, hostGroupName);
                    nodeList = new ArrayList<>();
                    autoRecoveryNodesMap.put(hostGroupName, nodeList);
                }
                nodeList.add(failedNode);
                autoRecoveryHostMetadata.put(failedNode, hostMetadata);
            } else if (hostGroup.getRecoveryMode() == RecoveryMode.MANUAL) {
                failedHostMetadata.put(failedNode, hostMetadata);
            }
        }
        if (!autoRecoveryNodesMap.isEmpty()) {
            flowManager.triggerClusterRepairFlow(stackId, autoRecoveryNodesMap, false);
            String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_AUTORECOVERY_REQUESTED.code(),
                Collections.singletonList(autoRecoveryNodesMap));
            updateChangedHosts(cluster, autoRecoveryHostMetadata, HostMetadataState.HEALTHY, HostMetadataState.WAITING_FOR_REPAIR, recoveryMessage);
        }
        if (!failedHostMetadata.isEmpty()) {
            String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_FAILED_NODES_REPORTED.code(),
                Collections.singletonList(failedHostMetadata.keySet()));
            updateChangedHosts(cluster, failedHostMetadata, HostMetadataState.HEALTHY, HostMetadataState.UNHEALTHY, recoveryMessage);
        }
    }

    @Override
    public void repairCluster(Long stackId, List<String> repairedHostGroups, boolean removeOnly) {
        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());

        Map<String, List<String>> failedNodeMap = new HashMap<>();
        for (HostGroup hg : hostGroups) {
            List<String> failedNodes = new ArrayList<>();
            if (repairedHostGroups.contains(hg.getName()) && hg.getRecoveryMode() == RecoveryMode.MANUAL) {
                for (HostMetadata hmd : hg.getHostMetadata()) {
                    if (hmd.getHostMetadataState() == HostMetadataState.UNHEALTHY) {
                        validateRepair(stack, hmd);
                        if (!failedNodeMap.containsKey(hg.getName())) {
                            failedNodeMap.put(hg.getName(), failedNodes);
                        }
                        failedNodes.add(hmd.getHostName());
                    }
                }
            }
        }
        if (!failedNodeMap.isEmpty()) {
            flowManager.triggerClusterRepairFlow(stackId, failedNodeMap, removeOnly);
            String recoveryMessage = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_MANUALRECOVERY_REQUESTED.code(),
                Collections.singletonList(repairedHostGroups));
            LOGGER.info(recoveryMessage);
            eventService.fireCloudbreakEvent(stack.getId(), "RECOVERY", recoveryMessage);
        }
    }

    private void validateRepair(Stack stack, HostMetadata hostMetadata) {
        if (isGateway(hostMetadata) && !isMultipleGateway(stack)) {
            throw new BadRequestException("Ambari server failure cannot be repaired with single gateway!");
        }
        if (isGateway(hostMetadata) && withEmbeddedAmbariDB(stack.getCluster())) {
            throw new BadRequestException("Ambari server failure with embedded database cannot be repaired!");
        }
    }

    private boolean isGateway(HostMetadata hostMetadata) {
        return hostMetadata.getHostGroup().getConstraint().getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY;
    }

    private boolean withEmbeddedAmbariDB(Cluster cluster) {
        AmbariDatabase ambariDB = clusterComponentConfigProvider.getAmbariDatabase(cluster.getId());
        return ambariDB == null || DatabaseVendor.EMBEDDED.value().equals(ambariDB.getVendor());
    }

    private void updateChangedHosts(Cluster cluster, Map<String, HostMetadata> failedHostMetadata, HostMetadataState healthyState,
        HostMetadataState unhealthyState, String recoveryMessage) {
        Set<HostMetadata> hosts = hostMetadataRepository.findHostsInCluster(cluster.getId());
        Set<HostMetadata> changedHosts = new HashSet<>();
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
            LOGGER.info(recoveryMessage);
            eventService.fireCloudbreakEvent(cluster.getStack().getId(), "RECOVERY", recoveryMessage);
            hostMetadataRepository.save(changedHosts);
        }
    }

    private void sync(Stack stack) {
        flowManager.triggerClusterSync(stack.getId());
    }

    private void start(Stack stack, Cluster cluster) {
        if (stack.isStartInProgress()) {
            String message = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_START_REQUESTED.code());
            eventService.fireCloudbreakEvent(stack.getId(), START_REQUESTED.name(), message);
            updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        } else {
            if (cluster.isAvailable()) {
                String statusDesc = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_START_IGNORED.code());
                LOGGER.info(statusDesc);
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
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_STOP_IGNORED.code());
            LOGGER.info(statusDesc);
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

    @Override
    @Transactional(TxType.NEVER)
    public Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason);
        StackStatus stackStatus = stackService.getCurrentStatusByStackId(stackId);
        Cluster cluster = retrieveClusterByStackId(stackId);
        if (cluster != null) {
            cluster.setStatus(status);
            cluster.setStatusReason(statusReason);
            cluster = clusterRepository.save(cluster);
            if (status.isRemovableStatus()) {
                InMemoryStateStore.deleteCluster(cluster.getId());
                if (stackStatus.getStatus().isRemovableStatus()) {
                    InMemoryStateStore.deleteStack(stackId);
                }
            } else {
                InMemoryStateStore.putCluster(cluster.getId(), statusToPollGroupConverter.convert(status));
                if (InMemoryStateStore.getStack(stackId) == null) {
                    InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(stackStatus.getStatus()));
                }
            }
        }
        return cluster;
    }

    @Override
    @Transactional(TxType.NEVER)
    public Cluster updateClusterStatusByStackId(Long stackId, Status status) {
        return updateClusterStatusByStackId(stackId, status, "");
    }

    @Override
    @Transactional(TxType.NOT_SUPPORTED)
    public Cluster updateClusterStatusByStackIdOutOfTransaction(Long stackId, Status status) {
        return updateClusterStatusByStackId(stackId, status, "");
    }

    @Override
    @Transactional(TxType.NEVER)
    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    @Override
    @Transactional(TxType.NEVER)
    public Cluster updateClusterMetadata(Long stackId) {
        Stack stack = stackService.getById(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        try {
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stackId, cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
            Map<String, Integer> hostGroupCounter = new HashMap<>();
            Set<HostMetadata> hosts = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
            Map<String, String> hostStatuses = ambariClient.getHostStatuses();
            for (HostMetadata host : hosts) {
                if (hostStatuses.containsKey(host.getHostName())) {
                    String hgName = host.getHostGroup().getName();
                    Integer hgCounter = hostGroupCounter.getOrDefault(hgName, 0) + 1;
                    hostGroupCounter.put(hgName, hgCounter);
                    HostMetadataState newState = HostMetadataState.HEALTHY.name().equals(hostStatuses.get(host.getHostName()))
                        ? HostMetadataState.HEALTHY : HostMetadataState.UNHEALTHY;
                    boolean stateChanged = updateHostMetadataByHostState(stack, host.getHostName(), newState);
                    if (stateChanged && HostMetadataState.HEALTHY == newState) {
                        updateInstanceMetadataStateToRegistered(stackId, host);
                    }
                }
            }
            hostGroupCounter(cluster.getId(), hostGroupCounter);
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException(e);
        }
        return cluster;
    }

    private void hostGroupCounter(Long clusterId, Map<String, Integer> hostGroupCounter) {
        LOGGER.info("Counted hostgroups: {}", hostGroupCounter);
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(clusterId);
        for (HostGroup hostGroup : hostGroups) {
            Integer hgCounter = hostGroupCounter.getOrDefault(hostGroup.getName(), 0);
            if (!hgCounter.equals(hostGroup.getConstraint().getHostCount())) {
                hostGroup.getConstraint().setHostCount(hgCounter);
                constraintRepository.save(hostGroup.getConstraint());
                LOGGER.info("Updated HostCount for hostgroup: {}, counter: {}", hostGroup.getName(), hgCounter);
            }
        }
    }

    private void updateInstanceMetadataStateToRegistered(Long stackId, HostMetadata host) {
        InstanceMetaData instanceMetaData = instanceMetaDataRepository.findHostInStack(stackId, host.getHostName());
        if (instanceMetaData != null) {
            instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
            instanceMetadataRepository.save(instanceMetaData);
        }
    }

    @Override
    public Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, StackRepoDetails stackRepoDetails) {
        if (blueprintId == null || hostGroups == null) {
            throw new BadRequestException("Blueprint id and hostGroup assignments can not be null.");
        }
        Blueprint blueprint = blueprintService.get(blueprintId);
        Stack stack = stackService.getByIdWithLists(stackId);
        Cluster cluster = getCluster(stack);
        AmbariDatabase ambariDatabase = clusterComponentConfigProvider.getAmbariDatabase(cluster.getId());
        if (ambariDatabase != null && !DatabaseVendor.EMBEDDED.value().equals(ambariDatabase.getVendor())) {
            throw new BadRequestException("Ambari doesn't support resetting external DB automatically. To reset Ambari Server schema you must first drop "
                + "and then create it using DDL scripts from /var/lib/ambari-server/resources");
        }
        if (validateBlueprint) {
            blueprintValidator.validateBlueprintForStack(blueprint, hostGroups, stack.getInstanceGroups());
        }
        Boolean containerOrchestrator;
        try {
            containerOrchestrator = orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator();
        } catch (CloudbreakException ignored) {
            containerOrchestrator = false;
        }
        if (containerOrchestrator) {
            clusterTerminationService.deleteClusterContainers(cluster.getId());
            cluster = clusterRepository.findById(stack.getCluster().getId());
        }

        hostGroups = hostGroupService.saveOrUpdateWithMetadata(hostGroups, cluster);
        cluster = prepareCluster(hostGroups, stackRepoDetails, blueprint, stack, cluster);

        try {
            triggerClusterInstall(stack, cluster);
        } catch (CloudbreakException e) {
            throw new CloudbreakServiceException(e);
        }
        return stack.getCluster();

    }

    private Cluster prepareCluster(Set<HostGroup> hostGroups, StackRepoDetails stackRepoDetails, Blueprint blueprint, Stack stack, Cluster cluster) {
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().clear();
        cluster.getHostGroups().addAll(hostGroups);
        createHDPRepoComponent(stackRepoDetails, stack);
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        cluster.setStatus(REQUESTED);
        cluster.setStack(stack);
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    private Cluster getCluster(Stack stack) {
        Cluster cluster = clusterRepository.findById(stack.getCluster().getId());
        if (cluster == null) {
            throw new BadRequestException(String.format("Cluster does not exist on stack with '%s' id.", stack.getId()));
        }
        return cluster;
    }

    @Override
    public void upgrade(Long stackId, AmbariRepo ambariRepoUpgrade) {
        if (ambariRepoUpgrade != null) {
            Stack stack = stackService.getByIdWithLists(stackId);
            Cluster cluster = clusterRepository.findById(stack.getCluster().getId());
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
            if (ambariRepo == null) {
                try {
                    clusterComponentConfigProvider.store(new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS,
                        new Json(ambariRepoUpgrade), stack.getCluster()));
                } catch (JsonProcessingException ignored) {
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
                } catch (JsonProcessingException ignored) {
                    throw new BadRequestException(String.format("Ambari repo details cannot be saved. %s", ambariRepoUpgrade));
                }
            }
            try {
                flowManager.triggerClusterUpgrade(stack.getId());
            } catch (RuntimeException e) {
                throw new CloudbreakServiceException(e);
            }
        }
    }

    private void generateSignKeys(Gateway gateway) {
        if (gateway.getSignCert() == null) {
            KeyPair identityKey = PkiUtil.generateKeypair();
            KeyPair signKey = PkiUtil.generateKeypair();
            X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKey);

            gateway.setSignKey(PkiUtil.convert(identityKey.getPrivate()));
            gateway.setSignPub(PkiUtil.convert(identityKey.getPublic()));
            gateway.setSignCert(PkiUtil.convert(cert));
        }

    }

    private void createHDPRepoComponent(StackRepoDetails stackRepoDetailsUpdate, Stack stack) {
        if (stackRepoDetailsUpdate != null) {
            StackRepoDetails stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(stack.getCluster().getId());
            if (stackRepoDetails == null) {
                try {
                    ClusterComponent clusterComp = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, new Json(stackRepoDetailsUpdate), stack.getCluster());
                    clusterComponentConfigProvider.store(clusterComp);
                } catch (JsonProcessingException ignored) {
                    throw new BadRequestException(String.format("HDP Repo parameters cannot be converted. %s", stackRepoDetailsUpdate));
                }
            } else {
                ClusterComponent component = clusterComponentConfigProvider.getComponent(stack.getCluster().getId(), ComponentType.HDP_REPO_DETAILS);
                stackRepoDetails.setHdpVersion(stackRepoDetailsUpdate.getHdpVersion());
                stackRepoDetails.setVerify(stackRepoDetailsUpdate.isVerify());
                stackRepoDetails.setStack(stackRepoDetailsUpdate.getStack());
                stackRepoDetails.setUtil(stackRepoDetailsUpdate.getUtil());
                stackRepoDetails.setKnox(stackRepoDetailsUpdate.getKnox());
                try {
                    component.setAttributes(new Json(stackRepoDetails));
                    clusterComponentConfigProvider.store(component);
                } catch (JsonProcessingException ignored) {
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

    private boolean validateRequest(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        HostGroup hostGroup = getHostGroup(stack, hostGroupAdjustment);
        int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        boolean downScale = scalingAdjustment < 0;
        if (scalingAdjustment == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
        if (!downScale && hostGroup.getConstraint().getInstanceGroup() != null) {
            validateUnusedHosts(hostGroup.getConstraint().getInstanceGroup(), scalingAdjustment);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment);
            validateComponentsCategory(stack, hostGroupAdjustment.getHostGroup());
            if (hostGroupAdjustment.getWithStackUpdate() && hostGroupAdjustment.getScalingAdjustment() > 0) {
                throw new BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.");
            }
        }
        return downScale;
    }

    private void validateComponentsCategory(Stack stack, String hostGroup) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
        Blueprint blueprint = cluster.getBlueprint();
        try {
            JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
            String blueprintName = root.path("Blueprints").path("blueprint_name").asText();
            Map<String, String> categories = ambariClient.getComponentsCategory(blueprintName, hostGroup);
            for (Entry<String, String> entry : categories.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(MASTER_CATEGORY)) {
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
        Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetadataRepository.findUnusedHostsInInstanceGroup(instanceGroup.getId());
        if (unusedHostsInInstanceGroup.size() < scalingAdjustment) {
            throw new BadRequestException(String.format(
                "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                unusedHostsInInstanceGroup.size(), instanceGroup.getGroupName(), scalingAdjustment - unusedHostsInInstanceGroup.size()));
        }
    }

    private void validateRegisteredHosts(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        String hostGroup = hostGroupAdjustment.getHostGroup();
        int hostsCount = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroup).getHostMetadata().size();
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

    private HostGroup getHostGroup(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupAdjustment.getHostGroup());
        if (hostGroup == null) {
            throw new BadRequestException(String.format(
                "Invalid host group: cluster '%s' does not contain a host group named '%s'.",
                stack.getCluster().getName(), hostGroupAdjustment.getHostGroup()));
        }
        return hostGroup;
    }

    private boolean updateHostMetadataByHostState(Stack stack, String hostName, HostMetadataState newState) {
        boolean stateChanged = false;
        HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), hostName);
        HostMetadataState oldState = hostMetadata.getHostMetadataState();
        if (!oldState.equals(newState)) {
            stateChanged = true;
            hostMetadata.setHostMetadataState(newState);
            hostMetadataRepository.save(hostMetadata);
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_HOST_STATUS_UPDATED.code(), Arrays.asList(hostName, newState.name())));
        }
        return stateChanged;
    }

    @Override
    public <R extends ClusterResponse> R getClusterResponse(R response, String clusterJson) {
        response.setCluster(jsonHelper.createJsonFromString(clusterJson));
        return response;
    }

    @Override
    public Cluster getById(Long id) {
        Cluster cluster = clusterRepository.findOneWithLists(id);
        if (cluster == null) {
            throw new NotFoundException(String.format("Cluster '%s' not found", id));
        }
        return cluster;
    }

    @Override
    public ConfigsResponse retrieveOutputs(Long stackId, Set<BlueprintParameterJson> requests) throws CloudbreakSecuritySetupException, IOException {
        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), cluster.getAmbariIp());

        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);

        List<String> targets = new ArrayList<>();
        Map<String, String> bpI = new HashMap<>();
        if (cluster.getBlueprintInputs().getValue() != null) {
            bpI = cluster.getBlueprintInputs().get(Map.class);
        }
        prepareTargets(requests, targets, bpI);
        Map<String, String> results = new HashMap<>();
        if (cluster.getAmbariIp() != null) {
            results = ambariClient.getConfigValuesByConfigIds(targets);
        }
        prepareResults(requests, cluster, bpI, results);
        prepareAdditionalInputParameters(results, cluster);

        Set<BlueprintInputJson> blueprintInputJsons = new HashSet<>();

        for (Entry<String, String> stringStringEntry : results.entrySet()) {
            for (BlueprintParameterJson blueprintParameter : requests) {
                if (stringStringEntry.getKey().equals(blueprintParameter.getName())) {
                    BlueprintInputJson blueprintInputJson = new BlueprintInputJson();
                    blueprintInputJson.setName(blueprintParameter.getName());
                    blueprintInputJson.setPropertyValue(stringStringEntry.getValue());
                    blueprintInputJsons.add(blueprintInputJson);
                    break;
                }
            }
        }

        ConfigsResponse configsResponse = new ConfigsResponse();
        configsResponse.setInputs(blueprintInputJsons);
        return configsResponse;
    }

    @Override
    public Map<String, String> getHostStatuses(Long stackId) throws CloudbreakSecuritySetupException {
        AmbariClient ambariClient = getAmbariClient(stackId);
        return ambariClient.getHostStatuses();
    }

    private void prepareResults(Set<BlueprintParameterJson> requests, Cluster cluster, Map<String, String> bpI, Map<String, String> results) {
        if (cluster.getBlueprintInputs().getValue() != null) {
            if (bpI != null) {
                for (Entry<String, String> stringStringEntry : bpI.entrySet()) {
                    if (!results.keySet().contains(stringStringEntry.getKey())) {
                        results.put(stringStringEntry.getKey(), stringStringEntry.getValue());
                    }
                }
            }
        }

        for (BlueprintParameterJson request : requests) {
            if (results.keySet().contains(request.getReferenceConfiguration())) {
                results.put(request.getName(), results.get(request.getReferenceConfiguration()));
                results.remove(request.getReferenceConfiguration());
            }
        }
    }

    private void prepareTargets(Set<BlueprintParameterJson> requests, List<String> targets, Map<String, String> bpI) {
        for (BlueprintParameterJson request : requests) {
            if (bpI != null) {
                boolean contains = false;
                for (Entry<String, String> stringStringEntry : bpI.entrySet()) {
                    if (stringStringEntry.getKey().equals(request.getName())) {
                        contains = true;
                    }
                }
                if (!contains) {
                    targets.add(request.getReferenceConfiguration());
                }
            } else {
                targets.add(request.getReferenceConfiguration());
            }
        }
    }

    private void prepareAdditionalInputParameters(Map<String, String> results, Cluster cluster) {
        results.put("REMOTE_CLUSTER_NAME", cluster.getName());
    }

    private AmbariClient getAmbariClient(Long stackId) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.getByIdWithLists(stackId);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stackId, stack.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(httpClientConfig, stack.getGatewayPort(), stack.getCluster());
        return ambariClient;
    }

    private enum Msg {
        AMBARI_CLUSTER_START_IGNORED("ambari.cluster.start.ignored"),
        AMBARI_CLUSTER_STOP_IGNORED("ambari.cluster.stop.ignored"),
        AMBARI_CLUSTER_HOST_STATUS_UPDATED("ambari.cluster.host.status.updated"),
        AMBARI_CLUSTER_START_REQUESTED("ambari.cluster.start.requested"),
        AMBARI_CLUSTER_AUTORECOVERY_REQUESTED("ambari.cluster.autorecovery.requested"),
        AMBARI_CLUSTER_MANUALRECOVERY_REQUESTED("ambari.cluster.manualrecovery.requested"),
        AMBARI_CLUSTER_FAILED_NODES_REPORTED("ambari.cluster.failednodes.reported");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

}
