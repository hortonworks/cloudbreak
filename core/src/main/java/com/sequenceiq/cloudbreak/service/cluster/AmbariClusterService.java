package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.service.cluster.DataNodeUtils.sortByUsedSpace;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterDeleteRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterUserNamePasswordUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import groovyx.net.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AmbariClusterService implements ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterService.class);
    private static final String MASTER_CATEGORY = "MASTER";
    private static final String DATANODE = "DATANODE";
    private static final double SAFETY_PERCENTAGE = 1.2;

    @Inject
    private StackService stackService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private AmbariConfigurationService configurationService;

    @Inject
    private HostFilterService hostFilterService;

    @Inject
    private FlowManager flowManager;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    private enum Msg {
        AMBARI_CLUSTER_START_IGNORED("ambari.cluster.start.ignored"),
        AMBARI_CLUSTER_STOP_IGNORED("ambari.cluster.stop.ignored"),
        AMBARI_CLUSTER_HOST_STATUS_UPDATED("ambari.cluster.host.status.updated");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }


    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster create(CbUser user, Long stackId, Cluster cluster) {
        Stack stack = stackService.get(stackId);
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster()
                    .getName()));
        }
        if (clusterRepository.findByNameInAccount(cluster.getName(), user.getAccount()) != null) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName());
        }
        for (HostGroup hostGroup : cluster.getHostGroups()) {
            constraintRepository.save(hostGroup.getConstraint());
        }
        if (cluster.getFileSystem() != null) {
            fileSystemRepository.save(cluster.getFileSystem());
        }
        cluster.setStack(stack);
        cluster.setOwner(user.getUserId());
        cluster.setAccount(user.getAccount());
        stack.setCluster(cluster);
        try {
            cluster = clusterRepository.save(cluster);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName(), ex);
        }
        if (stack.isAvailable()) {
            flowManager.triggerClusterInstall(new ProvisionRequest(platform(stack.cloudPlatform()), stack.getId()));
        }
        return cluster;
    }

    @Override
    public void delete(CbUser user, Long stackId) {
        Stack stack = stackService.get(stackId);
        LOGGER.info("Cluster delete requested.");
        if (!user.getUserId().equals(stack.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("Clusters can only be deleted by account admins or owners.");
        }
        if (Status.DELETE_COMPLETED.equals(stack.getCluster().getStatus())){
            throw new BadRequestException("Clusters is already deleted.");
        }
        ClusterDeleteRequest clusterDeleteRequest = new ClusterDeleteRequest(stackId, platform(stack.cloudPlatform()), stack.getCluster().getId());
        flowManager.triggerClusterTermination(clusterDeleteRequest);
    }

    @Override
    public Cluster retrieveClusterByStackId(Long stackId) {
        return stackService.findLazy(stackId).getCluster();
    }

    @Override
    public ClusterResponse retrieveClusterForCurrentUser(Long stackId) {
        Stack stack = stackService.get(stackId);
        return conversionService.convert(stack.getCluster(), ClusterResponse.class);
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateAmbariIp(Long clusterId, String ambariIp) {
        Cluster cluster = clusterRepository.findById(clusterId);
        cluster.setAmbariIp(ambariIp);
        cluster = clusterRepository.save(cluster);
        LOGGER.info("Updated cluster: [ambariIp: '{}'].", ambariIp);
        return cluster;
    }

    @Override
    public String getClusterJson(String ambariIp, Long stackId) {
        Stack stack = stackService.get(stackId);
        if (stack.getAmbariIp() == null) {
            throw new NotFoundException(String.format("Ambari server could not be available for the stack.[id: %s]", stackId));
        }
        Cluster cluster = stack.getCluster();
        try {
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
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
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    @Override
    public UpdateAmbariHostsRequest updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        boolean decommissionRequest = validateRequest(stack, hostGroupAdjustment);
        UpdateAmbariHostsRequest updateRequest;
        if (decommissionRequest) {
            updateRequest = new UpdateAmbariHostsRequest(stackId, hostGroupAdjustment, new HashSet<String>(),
                    collectDownscaleCandidates(hostGroupAdjustment, stack, cluster, decommissionRequest),
                    decommissionRequest, platform(stack.cloudPlatform()),
                    hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER);
            updateClusterStatusByStackId(stackId, UPDATE_REQUESTED);
            flowManager.triggerClusterDownscale(updateRequest);
        } else {
            updateRequest = new UpdateAmbariHostsRequest(stackId, hostGroupAdjustment,
                    collectUpscaleCandidates(hostGroupAdjustment, cluster), new ArrayList<HostMetadata>(),
                    decommissionRequest, platform(stack.cloudPlatform()),
                    ScalingType.UPSCALE_ONLY_CLUSTER);
            flowManager.triggerClusterUpscale(updateRequest);
        }
        return updateRequest;
    }

    @Override
    public ClusterStatusUpdateRequest updateStatus(Long stackId, StatusRequest statusRequest) {
        ClusterStatusUpdateRequest retVal = null;

        Stack stack = stackService.get(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        switch (statusRequest) {
            case SYNC:
                sync(stack, cluster, statusRequest);
                break;
            case STOPPED:
                retVal = stop(stack, cluster, statusRequest);
                break;
            case STARTED:
                retVal = start(stack, cluster, statusRequest);
                break;
            default:
                throw new BadRequestException("Cannot update the status of cluster because status request not valid");
        }
        return retVal;
    }

    @Override
    public Cluster updateUserNamePassword(Long stackId, UserNamePasswordJson userNamePasswordJson) {
        Stack stack = stackService.get(stackId);
        flowManager.triggerClusterUserNamePasswordUpdate(
                new ClusterUserNamePasswordUpdateRequest(stack.getId(), userNamePasswordJson.getUserName(),
                        userNamePasswordJson.getPassword(), platform(stack.cloudPlatform())));
        return stack.getCluster();
    }

    private void sync(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        flowManager.triggerClusterSync(new ClusterStatusUpdateRequest(stack.getId(), statusRequest, platform(stack.cloudPlatform())));
    }

    private ClusterStatusUpdateRequest start(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        ClusterStatusUpdateRequest retVal = null;
        if (stack.isStartInProgress()) {
            retVal = new ClusterStatusUpdateRequest(stack.getId(), statusRequest, platform(stack.cloudPlatform()));
            flowManager.triggerClusterStartRequested(retVal);
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
                retVal = new ClusterStatusUpdateRequest(stack.getId(), statusRequest, platform(stack.cloudPlatform()));
                flowManager.triggerClusterStart(retVal);
            }
        }
        return retVal;
    }

    private ClusterStatusUpdateRequest stop(Stack stack, Cluster cluster, StatusRequest statusRequest) {
        ClusterStatusUpdateRequest retVal = null;
        if (cluster.isStopped()) {
            String statusDesc = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_STOP_IGNORED.code());
            LOGGER.info(statusDesc);
            eventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), statusDesc);
        } else if (stack.infrastructureIsEphemeral()) {
            throw new BadRequestException(
                    String.format("Cannot stop a cluster if the volumeType is Ephemeral.", cluster.getId()));
        } else if (!cluster.isClusterReadyForStop() && !cluster.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STOPPED, because it isn't in AVAILABLE state.", cluster.getId()));
        } else if (!stack.isStackReadyForStop() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", cluster.getId()));
        } else if (cluster.isAvailable() || cluster.isStopFailed()) {
            updateClusterStatusByStackId(stack.getId(), STOP_REQUESTED);
            retVal = new ClusterStatusUpdateRequest(stack.getId(), statusRequest, platform(stack.cloudPlatform()));
            flowManager.triggerClusterStop(retVal);
        }
        return retVal;
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason);
        Cluster cluster = stackService.findLazy(stackId).getCluster();
        if (cluster != null) {
            cluster.setStatus(status);
            cluster.setStatusReason(statusReason);
            cluster = clusterRepository.save(cluster);
        }
        return cluster;
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateClusterStatusByStackId(Long stackId, Status status) {
        return updateClusterStatusByStackId(stackId, status, "");
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateClusterUsernameAndPassword(Cluster cluster, String userName, String password) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster.setUserName(userName);
        cluster.setPassword(password);
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster updateClusterMetadata(Long stackId) {
        Stack stack = stackService.findLazy(stackId);
        if (stack.getCluster() == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        try {
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, stack.getCluster().getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, stack.getCluster().getUserName(), stack.getCluster().getPassword());
            Set<HostMetadata> hosts = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
            Map<String, String> hostStatuses = ambariClient.getHostStatuses();
            for (HostMetadata host : hosts) {
                updateHostMetadataByHostState(stack, host.getHostName(), hostStatuses);
            }
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException(e);
        }
        return stack.getCluster();
    }

    @Override
    @Transactional(Transactional.TxType.NEVER)
    public Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, AmbariStackDetails ambariStackDetails) {
        if (blueprintId == null || hostGroups == null) {
            throw new BadRequestException("Blueprint id and hostGroup assignments can not be null.");
        }
        Stack stack = stackService.get(stackId);
        Blueprint blueprint = blueprintService.get(blueprintId);
        if (blueprint == null) {
            throw new BadRequestException(String.format("Blueprint not exist with '%s' id.", blueprintId));
        }
        Cluster cluster = stack.getCluster();
        if (validateBlueprint) {
            blueprintValidator.validateBlueprintForStack(blueprint, hostGroups, stackService.get(stackId).getInstanceGroups());
        }
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().removeAll(cluster.getHostGroups());
        cluster.getHostGroups().addAll(hostGroups);
        if (ambariStackDetails != null) {
            cluster.setAmbariStackDetails(ambariStackDetails);
        }
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        cluster.setStatus(REQUESTED);
        clusterRepository.save(cluster);
        flowManager.triggerClusterReInstall(new ProvisionRequest(platform(stack.cloudPlatform()), stack.getId()));
        return stack.getCluster();
    }

    private boolean validateRequest(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        HostGroup hostGroup = getHostGroup(stack, hostGroupAdjustment);
        int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        boolean downScale = scalingAdjustment < 0;
        if (scalingAdjustment == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
        if (!downScale) {
            validateUnusedHosts(hostGroup, scalingAdjustment);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment);
            validateComponentsCategory(stack, hostGroupAdjustment);
            if (hostGroupAdjustment.getWithStackUpdate() && hostGroupAdjustment.getScalingAdjustment() > 0) {
                throw new BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.");
            }
        }
        return downScale;
    }

    private Set<String> collectUpscaleCandidates(HostGroupAdjustmentJson adjustmentJson, Cluster cluster) {
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), adjustmentJson.getHostGroup());
        // TODO: why instancegroups?
        Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetadataRepository.findUnusedHostsInInstanceGroup(hostGroup.getConstraint().getInstanceGroup().getId());
        Set<String> instanceIds = new HashSet<>();
        for (InstanceMetaData instanceMetaData : unusedHostsInInstanceGroup) {
            instanceIds.add(instanceMetaData.getPrivateIp());
            if (instanceIds.size() >= adjustmentJson.getScalingAdjustment()) {
                break;
            }
        }
        return instanceIds;
    }

    private List<HostMetadata> collectDownscaleCandidates(HostGroupAdjustmentJson adjustmentJson, Stack stack, Cluster cluster, boolean decommissionRequest)
            throws CloudbreakSecuritySetupException {
        List<HostMetadata> downScaleCandidates = new ArrayList<>();
        if (decommissionRequest) {
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
            int replication = getReplicationFactor(ambariClient, adjustmentJson.getHostGroup());
            HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), adjustmentJson.getHostGroup());
            Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
            List<HostMetadata> filteredHostList = hostFilterService.filterHostsForDecommission(stack, hostsInHostGroup, adjustmentJson.getHostGroup());
            int reservedInstances = hostsInHostGroup.size() - filteredHostList.size();
            verifyNodeCount(cluster, replication, adjustmentJson.getScalingAdjustment(), filteredHostList, reservedInstances);
            if (doesHostGroupContainDataNode(ambariClient, cluster.getBlueprint().getBlueprintName(), hostGroup.getName())) {
                downScaleCandidates = checkAndSortByAvailableSpace(stack, ambariClient, replication,
                        adjustmentJson.getScalingAdjustment(), filteredHostList);
            } else {
                downScaleCandidates = filteredHostList;
            }
        }
        return downScaleCandidates;
    }

    private int getReplicationFactor(AmbariClient ambariClient, String hostGroup) {
        try {
            Map<String, String> configuration = configurationService.getConfiguration(ambariClient, hostGroup);
            return Integer.parseInt(configuration.get(ConfigParam.DFS_REPLICATION.key()));
        } catch (ConnectException e) {
            LOGGER.error("Cannot connect to Ambari to get the configuration", e);
            throw new BadRequestException("Cannot connect to Ambari");
        }
    }

    private void validateComponentsCategory(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        String hostGroup = hostGroupAdjustment.getHostGroup();
        Blueprint blueprint = cluster.getBlueprint();
        try {
            JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
            String blueprintName = root.path("Blueprints").path("blueprint_name").asText();
            Map<String, String> categories = ambariClient.getComponentsCategory(blueprintName, hostGroup);
            for (String component : categories.keySet()) {
                if (categories.get(component).equalsIgnoreCase(MASTER_CATEGORY)) {
                    throw new BadRequestException(
                            String.format("Cannot downscale the '%s' hostGroupAdjustment group, because it contains a '%s' component", hostGroup, component));
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot check the host components category", e);
        }
    }

    private void validateUnusedHosts(HostGroup hostGroup, int scalingAdjustment) {
        // TODO: why instancegroups?
        Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetadataRepository.findUnusedHostsInInstanceGroup(hostGroup.getConstraint().getInstanceGroup().getId());
        if (unusedHostsInInstanceGroup.size() < scalingAdjustment) {
            throw new BadRequestException(String.format(
                    "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                    unusedHostsInInstanceGroup.size(), hostGroup.getConstraint().getInstanceGroup().getGroupName(), scalingAdjustment - unusedHostsInInstanceGroup.size()));
        }
    }

    private void validateRegisteredHosts(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        Set<HostMetadata> hostMetadata = hostGroupRepository.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupAdjustment.getHostGroup())
                .getHostMetadata();
        if (hostMetadata.size() <= -1 * hostGroupAdjustment.getScalingAdjustment()) {
            String errorMessage = String.format("[hostGroup: '%s', current hosts: %s, decommissions requested: %s]",
                    hostGroupAdjustment.getHostGroup(), hostMetadata.size(), -1 * hostGroupAdjustment.getScalingAdjustment());
            throw new BadRequestException(String.format(
                    "The host group must contain at least 1 host after the decommission: %s",
                    errorMessage));
        }
    }

    private HostGroup getHostGroup(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupAdjustment.getHostGroup());
        if (hostGroup == null) {
            throw new BadRequestException(String.format(
                    "Invalid host group: cluster '%s' does not contain a host group named '%s'.",
                    stack.getCluster().getName(), hostGroupAdjustment.getHostGroup()));
        }
        return hostGroup;
    }

    private void verifyNodeCount(Cluster cluster, int replication, int scalingAdjustment, List<HostMetadata> filteredHostList, int reservedInstances) {
        int adjustment = Math.abs(scalingAdjustment);
        int hostSize = filteredHostList.size();
        if (hostSize + reservedInstances - adjustment <= replication || hostSize < adjustment) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new BadRequestException("There is not enough node to downscale. "
                    + "Check the replication factor and the ApplicationMaster occupation.");
        }
    }

    private boolean doesHostGroupContainDataNode(AmbariClient client, String blueprint, String hostGroup) {
        return client.getBlueprintMap(blueprint).get(hostGroup).contains(DATANODE);
    }

    private List<HostMetadata> checkAndSortByAvailableSpace(Stack stack, AmbariClient client, int replication,
                                                            int adjustment, List<HostMetadata> filteredHostList) {
        int removeCount = Math.abs(adjustment);
        Map<String, Map<Long, Long>> dfsSpace = client.getDFSSpace();
        Map<String, Long> sortedAscending = sortByUsedSpace(dfsSpace, false);
        Map<String, Long> selectedNodes = selectNodes(sortedAscending, filteredHostList, removeCount);
        Map<String, Long> remainingNodes = removeSelected(sortedAscending, selectedNodes);
        LOGGER.info("Selected nodes for decommission: {}", selectedNodes);
        LOGGER.info("Remaining nodes after decommission: {}", remainingNodes);
        long usedSpace = getSelectedUsage(selectedNodes);
        long remainingSpace = getRemainingSpace(remainingNodes, dfsSpace);
        long safetyUsedSpace = ((Double) (usedSpace * replication * SAFETY_PERCENTAGE)).longValue();
        LOGGER.info("Checking DFS space for decommission, usedSpace: {}, remainingSpace: {}", usedSpace, remainingSpace);
        LOGGER.info("Used space with replication: {} and safety space: {} is: {}", replication, SAFETY_PERCENTAGE, safetyUsedSpace);
        if (remainingSpace < safetyUsedSpace) {
            throw new BadRequestException(
                    String.format("Trying to move '%s' bytes worth of data to nodes with '%s' bytes of capacity is not allowed", usedSpace, remainingSpace)
            );
        }
        return convert(selectedNodes, filteredHostList);
    }

    private Map<String, Long> selectNodes(Map<String, Long> sortedAscending, List<HostMetadata> filteredHostList, int removeCount) {
        Map<String, Long> select = new HashMap<>();
        int i = 0;
        for (String host : sortedAscending.keySet()) {
            if (i < removeCount) {
                for (HostMetadata hostMetadata : filteredHostList) {
                    if (hostMetadata.getHostName().equalsIgnoreCase(host)) {
                        select.put(host, sortedAscending.get(host));
                        i++;
                        break;
                    }
                }
            } else {
                break;
            }
        }
        return select;
    }

    private Map<String, Long> removeSelected(Map<String, Long> all, Map<String, Long> selected) {
        Map<String, Long> copy = new HashMap<>(all);
        for (String host : selected.keySet()) {
            Iterator<String> iterator = copy.keySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equalsIgnoreCase(host)) {
                    iterator.remove();
                    break;
                }
            }
        }
        return copy;
    }

    private long getSelectedUsage(Map<String, Long> selected) {
        long usage = 0;
        for (String host : selected.keySet()) {
            usage += selected.get(host);
        }
        return usage;
    }

    private long getRemainingSpace(Map<String, Long> remainingNodes, Map<String, Map<Long, Long>> dfsSpace) {
        long remaining = 0;
        for (String host : remainingNodes.keySet()) {
            Map<Long, Long> space = dfsSpace.get(host);
            remaining += space.keySet().iterator().next();
        }
        return remaining;
    }

    private List<HostMetadata> convert(Map<String, Long> selectedNodes, List<HostMetadata> filteredHostList) {
        List<HostMetadata> result = new ArrayList<>();
        for (String host : selectedNodes.keySet()) {
            for (HostMetadata hostMetadata : filteredHostList) {
                if (hostMetadata.getHostName().equalsIgnoreCase(host)) {
                    result.add(hostMetadata);
                    break;
                }
            }
        }
        return result;
    }

    private void updateHostMetadataByHostState(Stack stack, String hostName, Map<String, String> hostStatuses) {
        if (hostStatuses.containsKey(hostName)) {
            String hostState = hostStatuses.get(hostName);
            HostMetadata hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), hostName);
            HostMetadataState oldState = hostMetadata.getHostMetadataState();
            HostMetadataState newState = HostMetadataState.HEALTHY.name().equals(hostState) ? HostMetadataState.HEALTHY : HostMetadataState.UNHEALTHY;
            if (!oldState.equals(newState)) {
                hostMetadata.setHostMetadataState(newState);
                hostMetadataRepository.save(hostMetadata);
                eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                        cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_HOST_STATUS_UPDATED.code(), Arrays.asList(hostName, newState.name())));
            }
        }
    }

    public ClusterResponse getClusterResponse(ClusterResponse response, String clusterJson) {
        response.setCluster(jsonHelper.createJsonFromString(clusterJson));
        return response;
    }

    @Override
    public Cluster getById(Long id) {
        Cluster cluster = clusterRepository.findOne(id);
        if (cluster == null) {
            throw new NotFoundException(String.format("Cluster '%s' not found", id));
        }
        return cluster;
    }

}
