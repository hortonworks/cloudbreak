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
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiHostTemplate;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.google.common.collect.Sets;
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
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
public class ClouderaManagerDecomissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDecomissioner.class);

    private static final String SUMMARY_REQUEST_VIEW = "SUMMARY";

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
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

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
                    verifyNodeCount(replication, removableHostsInHostGroup.size(), hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet().size(),
                            0, stack);
                }
            }
        } catch (ApiException ex) {
            throw new CloudbreakServiceException("Could not verify if nodes are removable or not", ex);
        }
    }

    public Set<InstanceMetaData> collectDownscaleCandidates(ApiClient client, Stack stack, HostGroup hostGroup, Integer scalingAdjustment,
            int defaultRootVolumeSize, Set<InstanceMetaData> instanceMetaDatasInStack) {
        LOGGER.debug("Collecting downscale candidates");
        Set<InstanceMetaData> instancesForHostGroup = instanceMetaDatasInStack.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceGroup().getGroupName().equals(hostGroup.getName()))
                .collect(Collectors.toSet());
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());
            int replication = hostGroupNodesAreDataNodes(hostTemplates, hostGroup.getName()) ? getReplicationFactor(client, stack.getName()) : 0;
            verifyNodeCount(replication, scalingAdjustment, instancesForHostGroup.size(), 0, stack);

            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName(), null, null);

            Set<InstanceMetaData> instancesToRemove = instancesForHostGroup.stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() == null)
                    .limit(Math.abs(scalingAdjustment))
                    .collect(Collectors.toSet());

            List<ApiHost> apiHosts = hostRefList.getItems().stream()
                    .filter(host -> instancesForHostGroup.stream()
                            .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                            .anyMatch(instanceMetaData -> instanceMetaData.getDiscoveryFQDN().equals(host.getHostname())))
                    .map(ApiHostRef::getHostId)
                    .parallel()
                    .map(readHostSummary(hostsResourceApi))
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

    public Map<String, InstanceMetaData> collectHostsToRemove(Stack stack, HostGroup hostGroup, Set<String> hostNames, ApiClient client) {
        Set<InstanceMetaData> hostsInHostGroup = hostGroup.getInstanceGroup().getRunningInstanceMetaDataSet();
        Map<String, InstanceMetaData> hostsToRemove = hostsInHostGroup.stream()
                .filter(hostMetadata -> hostNames.contains(hostMetadata.getDiscoveryFQDN()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, hostMetadata -> hostMetadata));
        if (hostsToRemove.size() != hostNames.size()) {
            LOGGER.debug("Not all hosts found in the given host group. [{}, {}]", hostGroup.getName(), hostNames);
        }
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName(), null, null);
            List<String> runningHosts = hostRefList.getItems().stream()
                    .map(ApiHostRef::getHostname)
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
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName(), null, null);
            List<String> stillAvailableRemovableHosts = hostRefList.getItems().stream()
                    .filter(apiHostRef -> hostsToRemove.containsKey(apiHostRef.getHostname()))
                    .parallel()
                    .map(ApiHostRef::getHostname)
                    .collect(Collectors.toList());

            LOGGER.debug("Decommissioning nodes: [{}]", stillAvailableRemovableHosts);
            ClouderaManagerResourceApi apiInstance = new ClouderaManagerResourceApi(client);
            ApiHostNameList body = new ApiHostNameList().items(stillAvailableRemovableHosts);
            ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmHostDecommissioning(stack, client, apiCommand.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for host decommission");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakServiceException("Timeout while Cloudera Manager decommissioned host.");
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

    private Function<String, ApiHost> readHostSummary(HostsResourceApi hostsResourceApi) {
        return hostId -> {
            try {
                return hostsResourceApi.readHost(hostId, SUMMARY_REQUEST_VIEW);
            } catch (ApiException e) {
                LOGGER.error("Failed to read host: {}", hostId, e);
                throw new CloudbreakServiceException(e.getMessage(), e);
            }
        };
    }

    public void deleteHost(Stack stack, InstanceMetaData data, ApiClient client) {
        LOGGER.debug("Deleting host: [{}]", data.getDiscoveryFQDN());
        deleteRolesFromHost(stack, data, client);
        deleteHostFromClouderaManager(stack, data, client);
        deleteUnusedCredentialsFromCluster(stack, data, client);
    }

    private void deleteUnusedCredentialsFromCluster(Stack stack, InstanceMetaData data, ApiClient client) {
        LOGGER.debug("Deleting unused credentials");
        ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);
        try {
            ApiCommand command = clouderaManagerResourceApi.deleteCredentialsCommand("unused");
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, command.getId());
        } catch (ApiException e) {
            LOGGER.error("Failed to delete credentials of host: {}", data.getDiscoveryFQDN(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteHostFromClouderaManager(Stack stack, InstanceMetaData data, ApiClient client) {
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName(), null, null);
            Optional<ApiHostRef> hostRefOptional = hostRefList.getItems().stream()
                    .filter(host -> data.getDiscoveryFQDN().equals(host.getHostname()))
                    .findFirst();
            if (hostRefOptional.isPresent()) {
                ApiHostRef hostRef = hostRefOptional.get();
                HostsResourceApi hostsResourceApi = new HostsResourceApi(client);
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
        ServicesResourceApi servicesResourceApi = new ServicesResourceApi(client);
        RolesResourceApi rolesResourceApi = new RolesResourceApi(client);

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
        MgmtServiceResourceApi mgmtServiceResourceApi = new MgmtServiceResourceApi(client);
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
