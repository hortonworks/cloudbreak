package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

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
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
public class ClouderaManagerDecomissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDecomissioner.class);

    private static final String SUMMARY_REQUEST_VIEW = "SUMMARY";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private final Comparator<? super ApiHost> hostHealthComparator = (host1, host2) -> {
        boolean host1Healthy = ApiHealthSummary.GOOD.equals(host1.getHealthSummary());
        boolean host2Healthy = ApiHealthSummary.GOOD.equals(host2.getHealthSummary());

        if (host1Healthy && host2Healthy) {
            return host2.getHostname().compareTo(host1.getHostname());
        } else if (host1Healthy) {
            return 1;
        } else {
            return -1;
        }
    };

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private FlowMessageService flowMessageService;

    public void verifyNodesAreRemovable(Stack stack, Collection<InstanceMetaData> removableInstances, ApiClient client) {
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());

            for (HostGroup hostGroup : stack.getCluster().getHostGroups()) {
                Set<InstanceMetaData> removableHostsInHostGroup = removableInstances.stream()
                        .filter(instanceMetaData -> instanceMetaData.getInstanceGroup().getGroupName().equals(hostGroup.getName()))
                        .collect(Collectors.toSet());
                if (!removableHostsInHostGroup.isEmpty()) {
                    String hostGroupName = hostGroup.getName();
                    int replication = hostGroupNodesAreDataNodes(hostTemplates, hostGroupName) ? getReplicationFactor(client, stack.getName()) : 0;

                    Set<InstanceMetaData> runningInstances = hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet();
                    int removableSizeFromTheRunning = Math.toIntExact(removableHostsInHostGroup.stream()
                            .filter(instance -> runningInstances.contains(instance))
                            .count());
                    verifyNodeCount(replication, removableSizeFromTheRunning, runningInstances.size(),
                            0, stack);
                }
            }
        } catch (ApiException ex) {
            throw new CloudbreakServiceException("Could not verify if nodes are removable or not", ex);
        }
    }

    public Set<InstanceMetaData> collectDownscaleCandidates(ApiClient client, Stack stack, HostGroup hostGroup, Integer scalingAdjustment,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        LOGGER.debug("Collecting downscale candidates");
        Set<InstanceMetaData> instancesForHostGroup = instanceMetaDatasInStack.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceGroup().getGroupName().equals(hostGroup.getName()))
                .collect(Collectors.toSet());
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());
            int replication = hostGroupNodesAreDataNodes(hostTemplates, hostGroup.getName()) ? getReplicationFactor(client, stack.getName()) : 0;
            verifyNodeCount(replication, scalingAdjustment, instancesForHostGroup.size(), 0, stack);

            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            Set<InstanceMetaData> instancesToRemove = getUnusedInstances(scalingAdjustment, instancesForHostGroup, hostRefList);

            List<ApiHost> apiHosts = hostRefList.getItems().stream()
                    .filter(host -> instancesForHostGroup.stream()
                            .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                            .anyMatch(instanceMetaData -> instanceMetaData.getDiscoveryFQDN().equals(host.getHostname())))
                    .collect(Collectors.toList());

            Set<String> hostsToRemove = apiHosts.stream()
                    .sorted(hostHealthComparator)
                    .limit(Math.abs(scalingAdjustment) - instancesToRemove.size())
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toSet());

            Set<InstanceMetaData> clouderaManagerNodesToRemove = instancesForHostGroup.stream()
                    .filter(instanceMetaData -> hostsToRemove.contains(instanceMetaData.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            instancesToRemove.addAll(clouderaManagerNodesToRemove);

            LOGGER.debug("Downscale candidates: [{}]", instancesToRemove);
            return instancesToRemove;
        } catch (ApiException e) {
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Set<InstanceMetaData> getUnusedInstances(Integer scalingAdjustment, Set<InstanceMetaData> instancesForHostGroup, ApiHostList hostRefList) {
        Set<InstanceMetaData> instancesWithoutFQDN = getInstancesWithoutFQDN(scalingAdjustment, instancesForHostGroup);
        LOGGER.warn("Instances without FQDN: {}", instancesWithoutFQDN);
        Set<InstanceMetaData> instancesNotKnownByCM = getInstancesNotKnownByCM(instancesForHostGroup, hostRefList);
        addInstancesNotKnownByCMWithLimit(Math.abs(scalingAdjustment) - instancesWithoutFQDN.size(), instancesWithoutFQDN, instancesNotKnownByCM);
        return instancesWithoutFQDN;
    }

    private void addInstancesNotKnownByCMWithLimit(Integer limit, Set<InstanceMetaData> instancesToRemove, Set<InstanceMetaData> instancesNotKnownByCM) {
        instancesToRemove.addAll(instancesNotKnownByCM.stream().limit(limit).collect(Collectors.toSet()));
    }

    private Set<InstanceMetaData> getInstancesNotKnownByCM(Set<InstanceMetaData> instancesForHostGroup, ApiHostList hostRefList) {
        Set<InstanceMetaData> instancesNotKnownByCM = instancesForHostGroup.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceMetaData -> hostRefList.getItems().stream()
                        .noneMatch(apiHostRef -> instanceMetaData.getDiscoveryFQDN().equals(apiHostRef.getHostname())))
                .collect(Collectors.toSet());

        LOGGER.warn("Instances not known by CM: {}", instancesNotKnownByCM);
        return instancesNotKnownByCM;
    }

    private Set<InstanceMetaData> getInstancesWithoutFQDN(Integer scalingAdjustment, Set<InstanceMetaData> instancesForHostGroup) {
        return instancesForHostGroup.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() == null)
                .limit(Math.abs(scalingAdjustment))
                .collect(Collectors.toSet());
    }

    public Map<String, InstanceMetaData> collectHostsToRemove(Stack stack, HostGroup hostGroup, Set<String> hostNames, ApiClient client) {
        Set<InstanceMetaData> hostsInHostGroup = hostGroup.getInstanceGroup().getNotTerminatedInstanceMetaDataSet();
        Map<String, InstanceMetaData> hostsToRemove = hostsInHostGroup.stream()
                .filter(hostMetadata -> hostNames.contains(hostMetadata.getDiscoveryFQDN()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, hostMetadata -> hostMetadata));
        if (hostsToRemove.size() != hostNames.size()) {
            LOGGER.debug("Not all hosts found in the given host group. [{}, {}]", hostGroup.getName(), hostNames);
        }
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> runningHosts = hostRefList.getItems().stream()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());
            // TODO: what if i remove a node from CM manually?
            Sets.newHashSet(hostsToRemove.keySet()).stream()
                    .filter(hostName -> !runningHosts.contains(hostName))
                    .forEach(hostsToRemove::remove);
            LOGGER.debug("Collected hosts to remove: [{}]", hostsToRemove);
            return hostsToRemove;
        } catch (ApiException e) {
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Set<String> decommissionNodes(Stack stack, Map<String, InstanceMetaData> hostsToRemove, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            List<String> stillAvailableRemovableHosts = hostRefList.getItems().stream()
                    .filter(apiHostRef -> hostsToRemove.containsKey(apiHostRef.getHostname()))
                    .parallel()
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toList());

            LOGGER.debug("Decommissioning nodes: [{}]", stillAvailableRemovableHosts);
            boolean onlyLostNodesAffected = hostsToRemove.values().stream().allMatch(InstanceMetaData::isDeletedOnProvider);
            String hostGroupName = hostsToRemove.values().stream().map(instanceMetaData ->
                    instanceMetaData.getInstanceGroup().getGroupName()).findFirst().get();
            ClouderaManagerResourceApi apiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiHostNameList body = new ApiHostNameList().items(stillAvailableRemovableHosts);
            ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
            PollingResult pollingResult = clouderaManagerPollingServiceProvider
                    .startPollingCmHostDecommissioning(stack, client, apiCommand.getId(), onlyLostNodesAffected, stillAvailableRemovableHosts.size());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for host decommission");
            } else if (isTimeout(pollingResult)) {
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
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .collect(Collectors.toSet());
        } catch (ApiException e) {
            LOGGER.error("Failed to decommission hosts: {}", hostsToRemove.keySet(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void retryDecommissionNodes(ClouderaManagerResourceApi apiInstance, ApiHostNameList body, Stack stack, ApiClient client,
            List<String> removableHosts, String hostGroupName) throws ApiException {
        ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider
            .startPollingCmHostDecommissioning(stack, client, apiCommand.getId(), true, removableHosts.size());
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for host decommission");
        } else if (isTimeout(pollingResult)) {
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

    private void verifyNodeCount(int replication, int scalingAdjustment, int hostSize, int reservedInstances, Stack stack) {
        boolean repairInProgress = stack.getDiskResources().stream()
                .map(resource -> resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class))
                .flatMap(Optional::stream)
                .map(VolumeSetAttributes::getDeleteOnTermination)
                .anyMatch(Boolean.FALSE::equals);
        int adjustment = Math.abs(scalingAdjustment);
        if (!repairInProgress && (hostSize + reservedInstances - adjustment < replication || hostSize < adjustment)) {
            LOGGER.info("Cannot downscale: replication: {}, adjustment: {}, filtered host size: {}", replication, scalingAdjustment, hostSize);
            throw new NotEnoughNodeException("There is not enough node to downscale. Check the replication factor.");
        }
    }

    public void deleteHost(Stack stack, InstanceMetaData data, ApiClient client) {
        LOGGER.debug("Deleting host: [{}]", data.getDiscoveryFQDN());
        deleteRolesFromHost(stack, data, client);
        deleteHostFromClouderaManager(stack, data, client);
    }

    public void deleteUnusedCredentialsFromCluster(Stack stack, ApiClient client) {
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

    private void deleteHostFromClouderaManager(Stack stack, InstanceMetaData data, ApiClient client) {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList hostRefList = hostsResourceApi.readHosts(null, null, SUMMARY_REQUEST_VIEW);
            Optional<ApiHost> hostRefOptional = hostRefList.getItems().stream()
                    .filter(host -> data.getDiscoveryFQDN().equals(host.getHostname()))
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

    private void deleteRolesFromHost(Stack stack, InstanceMetaData data, ApiClient client) {
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

    public void stopAndRemoveMgmtService(Stack stack, ApiClient client) {
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

    private Consumer<ApiRole> deleteServiceRole(Stack stack, RolesResourceApi rolesResourceApi) {
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

    private Function<String, Stream<? extends ApiRole>> toRoleStream(Stack stack, RolesResourceApi rolesResourceApi, String filter) {
        return serviceName -> {
            try {
                return rolesResourceApi.readRoles(stack.getName(), serviceName, filter, SUMMARY_REQUEST_VIEW).getItems().stream();
            } catch (ApiException e) {
                LOGGER.error("Failed to read roles: service: {} filter: {}", serviceName, filter, e);
                throw new CloudbreakServiceException(e.getMessage(), e);
            }
        };
    }
}
