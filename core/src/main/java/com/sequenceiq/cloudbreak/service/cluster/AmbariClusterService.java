package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.service.cluster.DataNodeUtils.sortByUsedSpace;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.filter.HostFilterService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterService implements ClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterService.class);
    private static final String MASTER_CATEGORY = "MASTER";
    private static final String DATANODE = "DATANODE";
    private static final double SAFETY_PERCENTAGE = 1.2;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    @Autowired
    private AmbariConfigurationService configurationService;

    @Autowired
    private HostFilterService hostFilterService;

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private BlueprintValidator blueprintValidator;

    @Override
    public Cluster create(CbUser user, Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findOne(stackId);
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        if (stack.getCluster() != null) {
            throw new BadRequestException(String.format("A cluster is already created on this stack! [cluster: '%s']", stack.getCluster()
                    .getName()));
        }
        cluster.setOwner(user.getUserId());
        cluster.setAccount(user.getAccount());
        try {
            cluster = clusterRepository.save(cluster);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER, cluster.getName(), ex);
        }
        stack = stackUpdater.updateStackCluster(stack.getId(), cluster);
        if (Status.AVAILABLE.equals(stack.getStatus())) {
            flowManager.triggerClusterInstall(new ProvisionRequest(stack.cloudPlatform(), stack.getId()));
        }
        return cluster;
    }

    @Override
    public Cluster retrieveClusterByStackId(Long stackId) {
        return stackRepository.findById(stackId).getCluster();
    }

    @Override
    public Cluster retrieveClusterForCurrentUser(Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        return stack.getCluster();
    }

    @Override
    public String getClusterJson(String ambariIp, Long stackId) {
        Stack stack = stackRepository.findOne(stackId);
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
        try {
            String clusterJson = ambariClient.getClusterAsJson();
            if (clusterJson == null) {
                throw new InternalServerException(String.format("Cluster response coming from Ambari server was null. [Ambari Server IP: '%s']", ambariIp));
            }
            return clusterJson;
        } catch (HttpResponseException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new NotFoundException("Ambari blueprint not found.", e);
            } else {
                throw new InternalServerException("Something went wrong", e);
            }
        }
    }

    @Override
    public UpdateAmbariHostsRequest updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        boolean decommissionRequest = validateRequest(stack, hostGroupAdjustment);
        List<HostMetadata> downScaleCandidates = collectDownscaleCandidates(hostGroupAdjustment, stack, cluster, decommissionRequest);
        UpdateAmbariHostsRequest updateRequest;
        if (decommissionRequest) {
            updateRequest = new UpdateAmbariHostsRequest(stackId, hostGroupAdjustment, downScaleCandidates,
                    decommissionRequest, stack.cloudPlatform(),
                    hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER);
            flowManager.triggerClusterDownscale(updateRequest);
        } else {
            updateRequest = new UpdateAmbariHostsRequest(stackId, hostGroupAdjustment, downScaleCandidates,
                    decommissionRequest, stack.cloudPlatform(), ScalingType.UPSCALE_ONLY_CLUSTER);
            flowManager.triggerClusterUpscale(updateRequest);
        }
        return updateRequest;
    }

    @Override
    public ClusterStatusUpdateRequest updateStatus(Long stackId, StatusRequest statusRequest) {
        ClusterStatusUpdateRequest retVal = null;

        Stack stack = stackRepository.findOne(stackId);
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("There is no cluster installed on stack '%s'.", stackId));
        }
        long clusterId = cluster.getId();
        Status clusterStatus = cluster.getStatus();
        Status stackStatus = stack.getStatus();
        if (statusRequest.equals(StatusRequest.STARTED)) {
            if (Status.START_IN_PROGRESS.equals(stackStatus)) {
                LOGGER.info("Stack is starting, set cluster state to: {}", Status.START_REQUESTED);
                cluster.setStatus(Status.START_REQUESTED);
                clusterRepository.save(cluster);
            } else {
                if (clusterNotReadyForStart(clusterStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of cluster '%s' to STARTED, because it isn't in STOPPED state.", clusterId));
                } else if (!Status.AVAILABLE.equals(stackStatus)) {
                    throw new BadRequestException(
                            String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", clusterId));
                }
                cluster.setStatus(Status.START_REQUESTED);
                clusterRepository.save(cluster);
                retVal = new ClusterStatusUpdateRequest(stack.getId(), statusRequest, stack.cloudPlatform());
                flowManager.triggerClusterStart(retVal);
            }
        } else {
            if (clusterNotReadyForStop(clusterStatus)) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STOPPED, because it isn't in AVAILABLE state.", clusterId));
            } else if (stackNotReadyForStop(stackStatus)) {
                throw new BadRequestException(
                        String.format("Cannot update the status of cluster '%s' to STARTED, because the stack is not AVAILABLE", clusterId));
            }
            if (Status.AVAILABLE.equals(clusterStatus)) {
                cluster.setStatus(Status.STOP_IN_PROGRESS);
                clusterRepository.save(cluster);
                retVal = new ClusterStatusUpdateRequest(stack.getId(), statusRequest, stack.cloudPlatform());
                flowManager.triggerClusterStop(retVal);
            }
        }

        return retVal;
    }

    private boolean stackNotReadyForStop(Status stackStatus) {
        return !Status.AVAILABLE.equals(stackStatus) && !Status.STOP_REQUESTED.equals(stackStatus);
    }

    private boolean clusterNotReadyForStop(Status clusterStatus) {
        return !Status.AVAILABLE.equals(clusterStatus) && !Status.STOPPED.equals(clusterStatus);
    }

    private boolean clusterNotReadyForStart(Status clusterStatus) {
        return !Status.STOPPED.equals(clusterStatus) && !Status.START_REQUESTED.equals(clusterStatus);
    }

    @Override
    public Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason) {
        LOGGER.debug("Updating cluster status. stackId: {}, status: {}, statusReason: {}", stackId, status, statusReason);
        Cluster cluster = stackRepository.findById(stackId).getCluster();
        cluster.setStatus(status);
        cluster.setStatusReason(statusReason);
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    @Override
    public Cluster updateCluster(Cluster cluster) {
        LOGGER.debug("Updating cluster. clusterId: {}", cluster.getId());
        cluster = clusterRepository.save(cluster);
        return cluster;
    }

    @Override
    public Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups) {
        if (blueprintId == null || hostGroups == null) {
            throw new BadRequestException("Blueprint id and hostGroup assignments can not be null.");
        }
        Stack stack = stackRepository.findOne(stackId);
        Blueprint blueprint = blueprintRepository.findOne(blueprintId);
        if (blueprint == null) {
            throw new BadRequestException(String.format("Blueprint not exist with '%s' id.", blueprintId));
        }
        Cluster cluster = stack.getCluster();
        blueprintValidator.validateBlueprintForStack(blueprint, hostGroups, stackRepository.findOne(stackId).getInstanceGroups());
        cluster.setBlueprint(blueprint);
        cluster.getHostGroups().removeAll(cluster.getHostGroups());
        cluster.getHostGroups().addAll(hostGroups);
        LOGGER.info("Cluster requested [BlueprintId: {}]", cluster.getBlueprint().getId());
        stack = stackUpdater.updateStackCluster(stack.getId(), cluster);
        flowManager.triggerClusterReInstall(new ProvisionRequest(stack.cloudPlatform(), stack.getId()));
        return stack.getCluster();
    }

    private boolean validateRequest(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        HostGroup hostGroup = getHostGroup(stack, hostGroupAdjustment);
        int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        boolean downScale = scalingAdjustment < 0;
        if (scalingAdjustment == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
        if (!downScale) {
            validateUnregisteredHosts(hostGroup, scalingAdjustment);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment);
            validateComponentsCategory(stack, hostGroupAdjustment);
            if (hostGroupAdjustment.getWithStackUpdate() && hostGroupAdjustment.getScalingAdjustment() > 0) {
                throw new BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.");
            }
        }
        return downScale;
    }

    private List<HostMetadata> collectDownscaleCandidates(HostGroupAdjustmentJson adjustmentJson, Stack stack, Cluster cluster, boolean decommissionRequest) {
        List<HostMetadata> downScaleCandidates = new ArrayList<>();
        if (decommissionRequest) {
            AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
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

    private void validateComponentsCategory(Stack stack, HostGroupAdjustmentJson hostGroupAdjustment) {
        AmbariClient ambariClient = ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword());
        Cluster cluster = stack.getCluster();
        String hostGroup = hostGroupAdjustment.getHostGroup();
        Blueprint blueprint = cluster.getBlueprint();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(blueprint.getBlueprintText());
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

    private void validateUnregisteredHosts(HostGroup hostGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unregisteredHosts = instanceMetadataRepository.findUnregisteredHostsInInstanceGroup(hostGroup.getInstanceGroup().getId());
        if (unregisteredHosts.size() < scalingAdjustment) {
            throw new BadRequestException(String.format(
                    "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                    unregisteredHosts.size(), hostGroup.getInstanceGroup().getGroupName(), scalingAdjustment - unregisteredHosts.size()));
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
}
