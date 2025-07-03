package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostTemplate;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiHostsToRemoveArgs;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.NodeIsBusyException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.status.HostServiceStatus;
import com.sequenceiq.cloudbreak.cluster.status.HostServiceStatuses;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class ClouderaManagerDecommissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDecommissioner.class);

    private static final String SUMMARY_REQUEST_VIEW = "SUMMARY";

    private static final String FULL_REQUEST_VIEW = "FULL";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private FlowMessageService flowMessageService;

    public void verifyNodesAreRemovable(StackDtoDelegate stack, Collection<InstanceMetadataView> removableInstances, ApiClient client) {
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());
            if (noRepairInProgress(stack)) {
                List<InstanceMetadataView> runningDataNodes = getRunningDataNodes(hostTemplates, stack.getInstanceGroupDtos());
                List<InstanceMetadataView> removableDataNodes = runningDataNodes.stream()
                        .filter(removableInstances::contains)
                        .toList();
                if (!removableDataNodes.isEmpty()) {
                    int replicationFactor = getReplicationFactor(client, stack.getName());
                    verifyDataNodeReplicationFactor(replicationFactor, removableDataNodes.size(), runningDataNodes.size());
                }
            }
            verifyNodeNotBusy(removableInstances, client);
        } catch (ApiException ex) {
            throw new CloudbreakServiceException("Could not verify if nodes are removable or not", ex);
        }
    }

    private List<InstanceMetadataView> getRunningDataNodes(ApiHostTemplateList hostTemplates, List<InstanceGroupDto> instanceGroups) {
        return instanceGroups
                .stream()
                .filter(instanceGroup -> hostGroupNodesAreDataNodes(hostTemplates, instanceGroup.getInstanceGroup().getGroupName()))
                .flatMap(instanceGroup -> instanceGroup.getRunningInstanceMetaData().stream())
                .toList();
    }

    private void verifyNodeNotBusy(Collection<InstanceMetadataView> removableInstances, ApiClient client) {
        Collection<String> hosts = removableInstances.stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(org.apache.commons.lang3.StringUtils::isNoneBlank)
                .collect(Collectors.toSet());
        HostServiceStatuses hostServiceStates = getHostServiceStatuses(hosts, client);
        if (hostServiceStates.anyHostBusy()) {
            Set<String> busyHostNames = hostServiceStates.getBusyHosts();
            LOGGER.info("Cannot downscale, some nodes are is BUSY state : {}", busyHostNames);
            throw new NodeIsBusyException(String.format("Node is in 'busy' state, cannot be decommissioned right now. " +
                    "Please try to remove the node later. Busy hosts: %s", busyHostNames));
        }
    }

    private HostServiceStatuses getHostServiceStatuses(Collection<String> checkHosts, ApiClient client) {
        List<ApiHost> apiHostList = getHostsFromCM(client);
        Map<HostName, HostServiceStatus> hostStates = apiHostList.stream()
                .filter(apiHost -> checkHosts.contains(apiHost.getHostname()))
                .collect(Collectors.toMap(
                        apiHost -> hostName(apiHost.getHostname()), this::getHostServiceStatusAggregate));
        LOGGER.debug("Query aggregated host service states: {}", hostStates);
        return new HostServiceStatuses(hostStates);
    }

    private HostServiceStatus getHostServiceStatusAggregate(ApiHost apiHost) {
        boolean stateAggregateNotBusy = apiHost.getRoleRefs().stream()
                .map(ApiRoleRef::getRoleStatus)
                .noneMatch(state -> state == ApiRoleState.BUSY);
        return stateAggregateNotBusy ? HostServiceStatus.OK : HostServiceStatus.BUSY;
    }

    private List<ApiHost> getHostsFromCM(ApiClient client) {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList apiHostList = api.readHosts(null, null, "FULL_WITH_HEALTH_CHECK_EXPLANATION");
            LOGGER.trace("Response from CM for readHosts call: {}", apiHostList);
            return apiHostList.getItems();
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    public Set<InstanceMetadataView> collectDownscaleCandidates(ApiClient client, StackDtoDelegate stack, String hostGroupName, Integer scalingAdjustment,
                                                                Set<InstanceMetadataView> instanceMetaDatasInStack) {
        LOGGER.debug("Collecting downscale candidates");
        Set<InstanceMetadataView> instancesForHostGroup = instanceMetaDatasInStack.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceGroupName().equals(hostGroupName))
                .collect(Collectors.toSet());
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());
            if (noRepairInProgress(stack)) {
                validateNodeCount(scalingAdjustment, instancesForHostGroup.size());
                if (hostGroupNodesAreDataNodes(hostTemplates, hostGroupName)) {
                    int replication = getReplicationFactor(client, stack.getName());
                    verifyDataNodeReplicationFactor(replication, scalingAdjustment, getRunningDataNodes(hostTemplates, stack.getInstanceGroupDtos()).size());
                }
            }

            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, FULL_REQUEST_VIEW);
            Set<InstanceMetadataView> instancesToRemove = getUnusedInstances(scalingAdjustment, instancesForHostGroup, hostRefList);

            List<ApiHost> apiHosts = hostRefList.getItems().stream()
                    .filter(host -> instancesForHostGroup.stream()
                            .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                            .anyMatch(instanceMetaData -> instanceMetaData.getDiscoveryFQDN().equals(host.getHostname())))
                    .toList();
            int numInstanceToRemove = Math.abs(scalingAdjustment) - instancesToRemove.size();
            if (numInstanceToRemove > 0) {
                Set<String> healthyHosts = apiHosts.stream().filter(host -> ApiHealthSummary.GOOD.equals(host.getHealthSummary()))
                        .map(ApiHost::getHostname)
                        .collect(Collectors.toSet());
                Set<InstanceMetadataView> usedInstances = instancesForHostGroup.stream()
                        .filter(instanceMetaData -> !instancesToRemove.contains(instanceMetaData))
                        .collect(Collectors.toSet());
                Set<InstanceMetadataView> usedInstancesToRemove = null;
                if (stack.getStack() != null && stack.getStack().isMultiAz()) {
                    usedInstancesToRemove = calculateDownscaleCandidatesForMultiAz(usedInstances, numInstanceToRemove, healthyHosts);
                } else {
                    usedInstancesToRemove = usedInstances.stream()
                            .sorted(healthComparator(healthyHosts))
                            .limit(numInstanceToRemove)
                            .collect(Collectors.toSet());
                }
                instancesToRemove.addAll(usedInstancesToRemove);
            }
            LOGGER.debug("Downscale candidates: [{}]", instancesToRemove
                    .stream()
                    .map(InstanceMetadataView::getInstanceId)
                    .collect(Collectors.toSet()));
            return instancesToRemove;
        } catch (ApiException e) {
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Set<InstanceMetadataView> getUnusedInstances(Integer scalingAdjustment, Set<InstanceMetadataView> instancesForHostGroup, ApiHostList hostRefList) {
        Set<InstanceMetadataView> instancesWithoutFQDN = getInstancesWithoutFQDN(scalingAdjustment, instancesForHostGroup);
        LOGGER.warn("Instances without FQDN: {}", instancesWithoutFQDN);
        Set<InstanceMetadataView> instancesNotKnownByCM = getInstancesNotKnownByCM(instancesForHostGroup, hostRefList);
        addInstancesNotKnownByCMWithLimit(Math.abs(scalingAdjustment) - instancesWithoutFQDN.size(), instancesWithoutFQDN, instancesNotKnownByCM);
        return instancesWithoutFQDN;
    }

    private void addInstancesNotKnownByCMWithLimit(Integer limit, Set<InstanceMetadataView> instancesToRemove, Set<InstanceMetadataView> instancesNotKnownByCM) {
        instancesToRemove.addAll(instancesNotKnownByCM.stream().limit(limit).collect(Collectors.toSet()));
    }

    private Set<InstanceMetadataView> getInstancesNotKnownByCM(Set<InstanceMetadataView> instancesForHostGroup, ApiHostList hostRefList) {
        Set<InstanceMetadataView> instancesNotKnownByCM = instancesForHostGroup.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceMetaData -> hostRefList.getItems().stream()
                        .noneMatch(apiHostRef -> instanceMetaData.getDiscoveryFQDN().equals(apiHostRef.getHostname())))
                .collect(Collectors.toSet());

        LOGGER.warn("Instances not known by CM: {}", instancesNotKnownByCM
                .stream()
                .map(InstanceMetadataView::getInstanceId)
                .collect(Collectors.toSet()));
        return instancesNotKnownByCM;
    }

    private Set<InstanceMetadataView> getInstancesWithoutFQDN(Integer scalingAdjustment, Set<InstanceMetadataView> instancesForHostGroup) {
        return instancesForHostGroup.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() == null)
                .limit(Math.abs(scalingAdjustment))
                .collect(Collectors.toSet());
    }

    public Map<String, InstanceMetadataView> collectHostsToRemove(StackDtoDelegate stack, String hostGroupName, Set<String> hostNames, ApiClient client) {
        List<InstanceMetadataView> hostsInHostGroup = stack.getInstanceGroupByInstanceGroupName(hostGroupName).getInstanceMetadataViews();
        Map<String, InstanceMetadataView> hostsToRemove = hostsInHostGroup.stream()
                .filter(hostMetadata -> hostNames.contains(hostMetadata.getDiscoveryFQDN()))
                .collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, hostMetadata -> hostMetadata));
        if (hostsToRemove.size() != hostNames.size()) {
            List<String> missingHosts = hostNames.stream().filter(h -> !hostsToRemove.containsKey(h)).collect(Collectors.toList());
            LOGGER.debug("Not all requested hosts found in CB for host group: {}. MissingCount={}, missingHosts=[{}]. Requested hosts: [{}]",
                    hostGroupName, missingHosts.size(), missingHosts, hostNames);
        }
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> runningHosts = hostRefList.getItems().stream()
                    .map(ApiHost::getHostname)
                    .toList();
            // TODO: what if i remove a node from CM manually?

            List<String> matchingCmHosts = hostsToRemove.keySet().stream()
                    .filter(runningHosts::contains)
                    .toList();
            Set<String> matchingCmHostSet = new HashSet<>(matchingCmHosts);

            if (matchingCmHosts.size() != hostsToRemove.size()) {
                List<String> missingHostsInCm = hostsToRemove.keySet().stream().filter(h -> !matchingCmHostSet.contains(h)).collect(Collectors.toList());

                LOGGER.debug("Not all requested hosts found in CM. MissingCount={}, missingHosts=[{}]. Requested hosts: [{}]",
                        missingHostsInCm.size(), missingHostsInCm, hostsToRemove.keySet());
            }

            Sets.newHashSet(hostsToRemove.keySet()).stream()
                    .filter(hostName -> !matchingCmHostSet.contains(hostName))
                    .forEach(hostsToRemove::remove);
            LOGGER.debug("Collected hosts to remove: [{}]", hostsToRemove.values()
                    .stream()
                    .map(InstanceMetadataView::getInstanceId)
                    .collect(Collectors.toSet()));
            return hostsToRemove;
        } catch (ApiException e) {
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public void enterMaintenanceMode(Set<String> hostList, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        String currentHostId = null;
        int successCount = 0;
        List<String> availableHostsIdsFromCm = null;
        LOGGER.debug("Attempting to put {} instances into CM maintenance mode", hostList == null ? 0 : hostList.size());
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<ApiHost> items = hostRefList.getItems();
            if (CollectionUtils.isNotEmpty(items)) {
                availableHostsIdsFromCm = items.stream()
                        .filter(apiHostRef -> hostList.contains(apiHostRef.getHostname()))
                        .parallel()
                        .map(ApiHost::getHostId)
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isNotEmpty(availableHostsIdsFromCm)) {
                for (String hostId : availableHostsIdsFromCm) {
                    currentHostId = hostId;
                    hostsResourceApi.enterMaintenanceMode(hostId);
                    successCount++;
                }
            } else {
                LOGGER.info("availableHostsIdsFromCm property null which means CM API could not return back any host.");
            }
            LOGGER.debug("Finished putting {} instances into CM maintenance mode. Initial request size: {}, CM availableCount: {}",
                    successCount, hostList == null ? 0 : hostList.size(), availableHostsIdsFromCm == null ? "null" : availableHostsIdsFromCm);
        } catch (ApiException e) {
            LOGGER.error("Failed while putting a node into maintenance mode. nodeId=" + currentHostId + ", successCount=" + successCount, e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Set<String> decommissionNodesStopStart(StackDtoDelegate stack, Map<String, InstanceMetadataView> hostsToRemove, ApiClient client,
            long pollingTimeout) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            LOGGER.trace("Target decommissionNodes: count={}, hosts=[{}]", hostsToRemove.size(), hostsToRemove.keySet());
            LOGGER.debug("hostsAvailableFromCM: count={}, hosts=[{}]", hostRefList.getItems().size(),
                    hostRefList.getItems().stream().map(ApiHost::getHostname));
            List<String> stillAvailableRemovableHosts = hostRefList.getItems().stream()
                    .filter(apiHostRef -> hostsToRemove.containsKey(apiHostRef.getHostname()))
                    .parallel()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());

            Set<String> hostsAvailableForDecommissionSet = new HashSet<>(stillAvailableRemovableHosts);
            List<String> cmHostsUnavailableForDecommission = hostsToRemove.keySet().stream()
                    .filter(h -> !hostsAvailableForDecommissionSet.contains(h)).collect(Collectors.toList());
            if (!cmHostsUnavailableForDecommission.isEmpty()) {
                LOGGER.info("Some decommission targets are unavailable in CM: TotalDecommissionTargetCount={}, unavailableInCMCount={}, unavailableInCm=[{}]",
                        hostsToRemove.size(), cmHostsUnavailableForDecommission.size(), cmHostsUnavailableForDecommission);
            }

            ClouderaManagerResourceApi apiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiHostNameList body = new ApiHostNameList().items(stillAvailableRemovableHosts);
            ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);

            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmHostsDecommission(
                    stack, client, apiCommand.getId(), pollingTimeout);
            if (pollingResult.isExited()) {
                throw new CancellationException("Cluster was terminated while waiting for host decommission");
            } else if (pollingResult.isTimeout()) {
                String warningMessage = "Cloudera Manager decommission host command {} polling timed out, " +
                        "thus we are aborting the decommission and we are retrying it for lost nodes once again.";
                abortDecommissionWithWarningMessage(apiCommand, client, warningMessage);
                throw new CloudbreakServiceException(
                        String.format("Timeout while Cloudera Manager decommissioned host. CM command Id: %s", apiCommand.getId()));
            }
            return stillAvailableRemovableHosts.stream()
                    .map(hostsToRemove::get)
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .map(InstanceMetadataView::getDiscoveryFQDN)
                    .collect(Collectors.toSet());
        } catch (ApiException e) {
            LOGGER.error("Failed to decommission hosts: {}", hostsToRemove.keySet(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Set<String> decommissionNodes(StackDtoDelegate stack, Map<String, InstanceMetadataView> hostsToRemove, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> stillAvailableRemovableHosts = hostRefList.getItems().stream()
                    .filter(apiHostRef -> hostsToRemove.containsKey(apiHostRef.getHostname()))
                    .parallel()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());

            LOGGER.debug("Decommissioning nodes: [{}]", stillAvailableRemovableHosts);
            boolean onlyLostNodesAffected = hostsToRemove.values().stream().allMatch(InstanceMetadataView::isDeletedOnProvider);
            String hostGroupName = hostsToRemove.values().stream().map(InstanceMetadataView::getInstanceGroupName).findFirst().get();
            ClouderaManagerResourceApi apiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiHostNameList body = new ApiHostNameList().items(stillAvailableRemovableHosts);
            ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                    .startPollingCmHostDecommissioning(stack, client, apiCommand.getId(), onlyLostNodesAffected, stillAvailableRemovableHosts.size());
            if (pollingResult.isExited()) {
                throw new CancellationException("Cluster was terminated while waiting for host decommission");
            } else if (pollingResult.isTimeout()) {
                if (onlyLostNodesAffected) {
                    String warningMessage = "Cloudera Manager decommission host command {} polling timed out, " +
                            "thus we are aborting the decommission and we are retrying it for lost nodes once again.";
                    abortDecommissionWithWarningMessage(apiCommand, client, warningMessage);
                    retryDecommissionNodes(apiInstance, body, stack, client, stillAvailableRemovableHosts, hostGroupName);
                } else {
                    throw new CloudbreakServiceException("Timeout while Cloudera Manager decommissioned host.");
                }
            }

            return stillAvailableRemovableHosts.stream()
                    .map(hostsToRemove::get)
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .map(InstanceMetadataView::getDiscoveryFQDN)
                    .collect(Collectors.toSet());
        } catch (ApiException e) {
            LOGGER.error("Failed to decommission hosts: {}", hostsToRemove.keySet(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void retryDecommissionNodes(ClouderaManagerResourceApi apiInstance, ApiHostNameList body, StackDtoDelegate stack, ApiClient client,
                                        List<String> removableHosts, String hostGroupName) throws ApiException {
        ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                .startPollingCmHostDecommissioning(stack, client, apiCommand.getId(), true, removableHosts.size());
        if (pollingResult.isExited()) {
            throw new CancellationException("Cluster was terminated while waiting for host decommission");
        } else if (pollingResult.isTimeout()) {
            String warningMessage = "Cloudera Manager retried decommission host command {} polling timed out again, thus we are aborting decommission " +
                    "and we are skipping it, since these lost nodes clearly cannot be decommissioned, data loss expected in this case.";
            abortDecommissionWithWarningMessage(apiCommand, client, warningMessage);
            flowMessageService.fireInstanceGroupEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), hostGroupName,
                    ResourceEvent.CLUSTER_LOST_NODE_DECOMMISSION_ABORTED_TWICE, String.join(",", removableHosts));
        }
    }

    private void abortDecommissionWithWarningMessage(ApiCommand apiCommand, ApiClient client, String warningMessage) throws ApiException {
        LOGGER.warn(warningMessage, apiCommand.getId());
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        commandsResourceApi.abortCommand(apiCommand.getId());
    }

    private int getReplicationFactor(ApiClient client, String clusterName) {
        try {
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            ApiServiceConfig apiServiceConfig = servicesResourceApi.readServiceConfig(clusterName, "hdfs", "full");
            Optional<ApiConfig> dfsReplicationConfig =
                    apiServiceConfig.getItems().stream().filter(apiConfig -> "dfs_replication".equals(apiConfig.getName())).findFirst();
            return dfsReplicationConfig.map(rc -> Integer.parseInt(StringUtils.isEmpty(rc.getValue()) ? rc.getDefault() : rc.getValue())).orElse(0);
        } catch (ApiException ex) {
            throw new CloudbreakServiceException(ex.getMessage(), ex);
        }
    }

    private boolean hostGroupNodesAreDataNodes(ApiHostTemplateList hostTemplates, String hostGroupName) {
        Optional<ApiHostTemplate> hostTemplate = hostTemplates.getItems().stream().filter(ht -> ht.getName().equals(hostGroupName)).findFirst();
        return hostTemplate.map(apiHostTemplate -> apiHostTemplate.getRoleConfigGroupRefs().stream()
                .anyMatch(rcg -> rcg.getRoleConfigGroupName().contains("DATANODE"))).orElse(false);
    }

    private void verifyDataNodeReplicationFactor(int replication, int scalingAdjustment, int hostSize) {
        int adjustment = Math.abs(scalingAdjustment);
        if (hostSize - adjustment < replication) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new NotEnoughNodeException("There is not enough node to downscale. Check the replication factor.");
        }
    }

    private void validateNodeCount(int scalingAdjustment, int hostSize) {
        int adjustment = Math.abs(scalingAdjustment);
        if (hostSize < adjustment) {
            LOGGER.info("Cannot downscale: adjustment: {}, filtered host size: {}", scalingAdjustment, hostSize);
            throw new NotEnoughNodeException("There is not enough node to downscale.");
        }
    }

    private boolean noRepairInProgress(StackDtoDelegate stack) {
        return stack.getDiskResources().stream()
                .map(resource -> resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class))
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getDeleteOnTermination)
                .noneMatch(Boolean.FALSE::equals);
    }

    public void deleteHost(StackDtoDelegate stack, InstanceMetadataView data, ApiClient client) {
        LOGGER.debug("Deleting host: [{}]", data.getDiscoveryFQDN());
        deleteRolesFromHost(stack, data, client);
        deleteHostFromClouderaManager(stack, data, client);
    }

    public void removeHostsFromCluster(StackDtoDelegate stack, List<InstanceMetadataView> hosts, ApiClient client) {
        List<String> hostFqdns = hosts.stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toList());
        LOGGER.debug("Deleting hosts: [{}]", Joiner.on(",").join(hostFqdns));
        removeHostsFromCluster(hostFqdns, stack, client);
    }

    public void deleteUnusedCredentialsFromCluster(StackDtoDelegate stack, ApiClient client) {
        LOGGER.debug("Deleting unused credentials");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
        try {
            ApiCommand command = clouderaManagerResourceApi.deleteCredentialsCommand("unused");
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, command.getId());
        } catch (ApiException e) {
            LOGGER.error("Failed to delete unused credentials", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void removeHostsFromCluster(List<String> hosts, StackDtoDelegate stack, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> knownDeletableHosts = hostRefList.getItems().stream()
                    .filter(host -> hosts.contains(host.getHostname()))
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());
            if (!knownDeletableHosts.isEmpty()) {
                ApiHostsToRemoveArgs body = new ApiHostsToRemoveArgs();
                body.hostsToRemove(knownDeletableHosts);
                body.deleteHosts(Boolean.TRUE);
                ApiCommand command = hostsResourceApi.removeHostsFromCluster(body);
                LOGGER.debug("Remove hosts from cluster request sent, hosts: {}", knownDeletableHosts);
                ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingRemoveHostsFromCluster(stack, client, command.getId());
                if (pollingResult.isExited()) {
                    throw new CancellationException("Cluster was terminated while waiting for hosts removal from CM.");
                } else if (pollingResult.isTimeout()) {
                    throw new CloudbreakServiceException("Timeout while Cloudera Manager tried to remove hosts from cluster. " +
                            "If this issue persist, please see the following page to tune HDFS settings: " +
                            "https://docs.cloudera.com/cloudera-manager/7.12.0/managing-clusters/topics/" +
                            "cm-tuning-hdfs-prior-to-decommissioning-datanodes.html");
                }
            } else {
                LOGGER.debug("Hosts already removed.");
            }
        } catch (ApiException e) {
            LOGGER.error("Failed to remove hosts: {}", Joiner.on(",").join(hosts), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteHostFromClouderaManager(StackDtoDelegate stack, InstanceMetadataView data, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            Optional<ApiHost> hostRefOptional = hostRefList.getItems().stream()
                    .filter(host -> data.getDiscoveryFQDN() != null && data.getDiscoveryFQDN().equals(host.getHostname()))
                    .findFirst();
            if (hostRefOptional.isPresent()) {
                ApiHost hostRef = hostRefOptional.get();
                ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
                clustersResourceApi.removeHost(stack.getName(), hostRef.getHostId());
                hostsResourceApi.deleteHost(hostRef.getHostId());
                LOGGER.debug("Host remove request sent. Host id: [{}]", hostRef.getHostId());
            } else {
                LOGGER.debug("Host already deleted.");
            }
        } catch (ApiException e) {
            LOGGER.error("Failed to delete host: {}", data.getDiscoveryFQDN(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteRolesFromHost(StackDtoDelegate stack, InstanceMetadataView data, ApiClient client) {
        LOGGER.debug("Deleting roles from host: [{}]", data.getDiscoveryFQDN());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);

        String filter = "hostname==" + data.getDiscoveryFQDN();

        try {
            ApiServiceList serviceList = servicesResourceApi.readServices(stack.getName(), SUMMARY_REQUEST_VIEW);
            serviceList.getItems().stream()
                    .map(ApiService::getName)
                    .flatMap(toRoleStream(stack, rolesResourceApi, filter))
                    .forEach(deleteServiceRole(stack, rolesResourceApi));
        } catch (ApiException e) {
            LOGGER.error("Failed to read services", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public void stopAndRemoveMgmtService(StackDtoDelegate stack, ApiClient client) {
        MgmtServiceResourceApi mgmtServiceResourceApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(client);
        try {
            clouderaManagerPollingServiceProvider.startPollingCmManagementServiceShutdown(stack,
                    client, mgmtServiceResourceApi.stopCommand().getId());
            mgmtServiceResourceApi.deleteCMS();
        } catch (ApiException e) {
            LOGGER.error("Failed to stop management services.", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public void stopRolesOnHosts(StackDtoDelegate stack, ApiClient v53Client, ApiClient v51Client, Set<String> hosts,
            boolean stopServicesGracefully)
            throws CloudbreakException {
        try {
            LOGGER.info("Stop roles on hosts: {}", hosts);
            List<String> hostsSelectedForStopping = getExistingHostsWithGoodHealth(v51Client, hosts);
            if (CollectionUtils.isNotEmpty(hostsSelectedForStopping)) {
                if (stopServicesGracefully && v53Client != null) {
                    stopServicesOnHosts(stack, v53Client, hostsSelectedForStopping);
                }
                stopRolesOnHosts(stack, v51Client, hostsSelectedForStopping);
            }
        } catch (ApiException e) {
            LOGGER.error("Failed to stop roles on nodes: {}", hosts, e);
            throw new CloudbreakException("Failed to stop roles on nodes: " + hosts, e);
        }
    }

    private List<String> getExistingHostsWithGoodHealth(ApiClient apiClient, Set<String> hosts) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, FULL_REQUEST_VIEW);
        LOGGER.info("Hosts from CM: {}", hostRefList);
        List<ApiHost> selectedHosts = hostRefList.getItems().stream()
                .filter(apiHost -> hosts.contains(apiHost.getHostname()))
                .toList();
        if (CollectionUtils.isEmpty(selectedHosts)) {
            LOGGER.info("No host is selected to stop services and roles.");
            return List.of();
        } else {
            List<String> hostsWithGoodHealth = selectedHosts
                    .stream()
                    .filter(apiHost -> ApiHealthSummary.GOOD.equals(apiHost.getHealthSummary()))
                    .map(ApiHost::getHostname)
                    .toList();
            if (CollectionUtils.isEmpty(hostsWithGoodHealth)) {
                LOGGER.info("No host is in good health therefore stopping services and roles will be skipped.");
            }
            return hostsWithGoodHealth;
        }
    }

    private void stopServicesOnHosts(StackDtoDelegate stack, ApiClient apiClient, List<String> hostsSelectedForStopping)
            throws ApiException {
        LOGGER.info("Stopping services gracefully on hosts {}", hostsSelectedForStopping);
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        List<BigDecimal> commands = new ArrayList<>();
        for (String hostName : hostsSelectedForStopping) {
            try {
                commands.add(hostsResourceApi.stopAllRolesOnNodeGracefully(hostName).getId());
            } catch (Exception e) {
                LOGGER.warn("Error while tried to stop services gracefully on host '{}'.", hostName, e);
            }
        }
        clouderaManagerPollingServiceProvider.startPollingCommandList(stack, apiClient, commands, "Stop services gracefully.");
    }

    private void stopRolesOnHosts(StackDtoDelegate stack, ApiClient client, List<String> hostsInGoodHealthForStopRolesCommand) throws ApiException {
        ApiHostNameList items = new ApiHostNameList().items(hostsInGoodHealthForStopRolesCommand);
        ApiCommand apiCommand = clouderaManagerApiFactory.getClouderaManagerResourceApi(client).hostsStopRolesCommand(items);
        ExtendedPollingResult extendedPollingResult =
                clouderaManagerPollingServiceProvider.startPollingStopRolesCommand(stack, client, apiCommand.getId());
        if (extendedPollingResult.isExited()) {
            throw new CancellationException("Cluster was terminated while waiting for stop roles on hosts");
        } else if (extendedPollingResult.isTimeout()) {
            throw new CloudbreakServiceException(
                    String.format("Cloudera Manager stop roles command {} timed out. CM command Id: %s", apiCommand.getId()));
        }
    }

    private Consumer<ApiRole> deleteServiceRole(StackDtoDelegate stack, RolesResourceApi rolesResourceApi) {
        return role -> {
            String serviceName = role.getServiceRef().getServiceName();
            try {
                rolesResourceApi.deleteRole(stack.getName(), role.getName(), serviceName);
            } catch (ApiException e) {
                LOGGER.error("Failed to delete role: service: {} role: {}", serviceName, role.getName(), e);
                throw new CloudbreakServiceException(e.getMessage(), e);
            }
        };
    }

    private Function<String, Stream<? extends ApiRole>> toRoleStream(StackDtoDelegate stack, RolesResourceApi rolesResourceApi, String filter) {
        return serviceName -> {
            try {
                return rolesResourceApi.readRoles(stack.getName(), serviceName, filter, SUMMARY_REQUEST_VIEW).getItems().stream();
            } catch (ApiException e) {
                LOGGER.error("Failed to read roles: service: {} filter: {}", serviceName, filter, e);
                throw new CloudbreakServiceException(e.getMessage(), e);
            }
        };
    }

    private Set<InstanceMetadataView> calculateDownscaleCandidatesForMultiAz(Set<InstanceMetadataView> allInstances,
            int numInstancesToRemove, Set<String> healthyHosts) {
        Map<String, Collection<InstanceMetadataView>> availabilityZoneToNodesMap = allInstances.stream()
                .collect(Multimaps.toMultimap(
                        InstanceMetadataView::getAvailabilityZone,
                        Function.identity(),
                        ArrayListMultimap::create)).asMap();
        LOGGER.debug("Availability Zone to Nodes mapping is {}", availabilityZoneToNodesMap);
        Set<InstanceMetadataView> instancesToRemove = new HashSet<>();
        for (int instanceCountToRemove = numInstancesToRemove; !availabilityZoneToNodesMap.isEmpty() && instanceCountToRemove > 0; instanceCountToRemove--) {
            Map.Entry<String, Collection<InstanceMetadataView>> availabilityZoneWithInstances = Collections.max(availabilityZoneToNodesMap.entrySet(),
                    Map.Entry.comparingByValue(comparatorForDownscaleNodesInMultiAz(healthyHosts)));
            LOGGER.debug("Node for downscale is present in {}", availabilityZoneWithInstances.getKey());
            Optional<InstanceMetadataView> instanceToDelete = availabilityZoneWithInstances.getValue().stream()
                    .sorted(healthComparator(healthyHosts))
                    .findFirst();
            instanceToDelete.ifPresent(instance -> {
                instancesToRemove.add(instance);
                availabilityZoneWithInstances.getValue().remove(instance);
            });
        }
        LOGGER.debug("Downscale candidates for multiAz stack are {}", instancesToRemove);
        return instancesToRemove;
    }

    private Comparator<Collection<InstanceMetadataView>> comparatorForDownscaleNodesInMultiAz(Set<String> healthyHosts) {
        return (instances1, instances2) -> {
            long numUnhealthyNodes1 = getNumberOfUnhealthyNodes(instances1, healthyHosts);
            long numUnhealthyNodes2 = getNumberOfUnhealthyNodes(instances2, healthyHosts);
            if (numUnhealthyNodes1 != numUnhealthyNodes2) {
                return (int) (numUnhealthyNodes1 - numUnhealthyNodes2);
            } else {
                return instances1.size() - instances2.size();
            }
        };
    }

    private long getNumberOfUnhealthyNodes(Collection<InstanceMetadataView> instances, Set<String> healthyHosts) {
        return instances.stream().filter(instance -> !healthyHosts.contains(instance.getDiscoveryFQDN())).count();
    }

    private Comparator<InstanceMetadataView> healthComparator(Set<String> healthyHosts) {
        return (instance1, instance2) -> {
            boolean instance1Healthy = healthyHosts.contains(instance1.getDiscoveryFQDN());
            boolean instance2Healthy = healthyHosts.contains(instance2.getDiscoveryFQDN());
            if (instance1Healthy && instance2Healthy) {
                return instance2.getDiscoveryFQDN().compareTo(instance1.getDiscoveryFQDN());
            } else if (instance1Healthy) {
                return 1;
            } else if (instance2Healthy) {
                return -1;
            } else {
                return instance2.getDiscoveryFQDN().compareTo(instance1.getDiscoveryFQDN());
            }
        };
    }
}
