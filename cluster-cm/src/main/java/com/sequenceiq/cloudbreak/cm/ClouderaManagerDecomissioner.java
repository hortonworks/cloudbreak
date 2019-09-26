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
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.NotEnoughNodeException;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.PollingResult;

@Component
public class ClouderaManagerDecomissioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDecomissioner.class);

    private static final String SUMMARY_REQUEST_VIEW = "SUMMARY";

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    private final Comparator<? super ApiHost> hostHealthComparator = (host1, host2) -> {
        boolean host1Healthy = ApiHealthSummary.GOOD.equals(host1.getHealthSummary());
        boolean host2Healthy = ApiHealthSummary.GOOD.equals(host2.getHealthSummary());

        if (host1Healthy && host2Healthy) {
            return 0;
        } else if (host1Healthy) {
            return 1;
        } else {
            return -1;
        }
    };

    public void verifyNodesAreRemovable(Stack stack, Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, ApiClient client) {
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerClientFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());

            for (HostGroup hostGroup : hostGroups) {
                Collection<HostMetadata> removableHostsInHostGroup = hostGroupWithInstances.get(hostGroup.getId());
                if (removableHostsInHostGroup != null && !removableHostsInHostGroup.isEmpty()) {
                    String hostGroupName = hostGroup.getName();
                    int replication = hostGroupNodesAreDataNodes(hostTemplates, hostGroupName) ? getReplicationFactor(client, stack.getName()) : 0;
                    verifyNodeCount(replication, removableHostsInHostGroup.size(), hostGroup.getHostMetadata().size(), 0, stack);
                }
            }
        } catch (ApiException ex) {
            throw new CloudbreakServiceException("Could not verify if nodes are removable or not", ex);
        }
    }

    public Set<String> collectDownscaleCandidates(ApiClient client, Stack stack, HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        LOGGER.debug("Collecting downscale candidates");
        List<String> hostsInHostGroup = hostGroup.getHostMetadata().stream().map(HostMetadata::getHostName).collect(Collectors.toList());
        ClustersResourceApi clustersResourceApi = clouderaManagerClientFactory.getClustersResourceApi(client);
        HostsResourceApi hostsResourceApi = clouderaManagerClientFactory.getHostsResourceApi(client);
        try {
            HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerClientFactory.getHostTemplatesResourceApi(client);
            ApiHostTemplateList hostTemplates = hostTemplatesResourceApi.readHostTemplates(stack.getName());
            int replication = hostGroupNodesAreDataNodes(hostTemplates, hostGroup.getName()) ? getReplicationFactor(client, stack.getName()) : 0;
            verifyNodeCount(replication, scalingAdjustment, hostGroup.getHostMetadata().size(), 0, stack);

            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName());
            List<ApiHost> apiHosts = hostRefList.getItems().stream()
                    .filter(host -> hostsInHostGroup.contains(host.getHostname()))
                    .map(ApiHostRef::getHostId)
                    .parallel()
                    .map(readHostSummary(hostsResourceApi))
                    .collect(Collectors.toList());

            Set<String> hostsToRemove = apiHosts.stream()
                    .sorted(hostHealthComparator)
                    .limit(Math.abs(scalingAdjustment))
                    .map(ApiHost::getHostname)
                    .collect(Collectors.toSet());

            LOGGER.debug("Downscale candidates: [{}]", hostsToRemove);
            return hostsToRemove;
        } catch (ApiException e) {
            LOGGER.error("Failed to get host list for cluster: {}", stack.getName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    public Map<String, HostMetadata> collectHostsToRemove(Stack stack, HostGroup hostGroup, Set<String> hostNames, ApiClient client) {
        Set<HostMetadata> hostsInHostGroup = hostGroup.getHostMetadata();
        Map<String, HostMetadata> hostsToRemove = hostsInHostGroup.stream().filter(hostMetadata -> hostNames.contains(hostMetadata.getHostName())).collect(
                Collectors.toMap(HostMetadata::getHostName, hostMetadata -> hostMetadata));
        if (hostsToRemove.size() != hostNames.size()) {
            LOGGER.debug("Not all hosts found in the given host group. [{}, {}]", hostGroup.getName(), hostNames);
        }
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName());
            List<String> runningHosts = hostRefList.getItems().stream()
                    .map(ApiHostRef::getHostname)
                    .collect(Collectors.toList());
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

    public Set<HostMetadata> decommissionNodes(Stack stack, Map<String, HostMetadata> hostsToRemove, ApiClient client) {
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName());
            List<String> stillAvailableRemovableHosts = hostRefList.getItems().stream()
                    .filter(apiHostRef -> hostsToRemove.keySet().contains(apiHostRef.getHostname()))
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

            Set<HostMetadata> decommissionedHosts = stillAvailableRemovableHosts.stream()
                    .map(hostsToRemove::get)
                    .collect(Collectors.toSet());

            return decommissionedHosts;
        } catch (ApiException e) {
            LOGGER.error("Failed to decommission hosts: {}", hostsToRemove.keySet(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private int getReplicationFactor(ApiClient client, String clusterName) {
        try {
            ServicesResourceApi servicesResourceApi = clouderaManagerClientFactory.getServicesResourceApi(client);
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
        if (hostTemplate.isPresent()) {
            return hostTemplate.get().getRoleConfigGroupRefs().stream()
                    .filter(rcg -> rcg.getRoleConfigGroupName().contains("DATANODE")).findFirst().isPresent();
        } else {
            return false;
        }
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

    private Consumer<ApiHost> sortHostsByHealth(Map<String, HostMetadata> hostsToRemove, Map<String, HostMetadata> unhealthyHosts,
            Map<String, HostMetadata> healthyHosts) {
        return host -> {
            String hostHealthSummary = host.getHealthSummary().getValue();
            HostMetadata hostMetadata = hostsToRemove.get(host.getHostname());
            if (ApiHealthSummary.GOOD.getValue().equals(hostHealthSummary)) {
                healthyHosts.put(host.getHostname(), hostMetadata);
            } else {
                unhealthyHosts.put(host.getHostname(), hostMetadata);
            }
        };
    }

    private Consumer<? super HostMetadata> decommissionNode(Stack stack, ApiClient client, Map<String, HostMetadata> deletedHosts) {
        return hostMetadata -> {
            LOGGER.debug("Decommissioning node: [{}]", hostMetadata.getHostName());
            ClouderaManagerResourceApi apiInstance = new ClouderaManagerResourceApi(client);
            ApiHostNameList body = new ApiHostNameList().addItemsItem(hostMetadata.getHostName());
            try {
                ApiCommand apiCommand = apiInstance.hostsDecommissionCommand(body);
                PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmHostDecommissioning(stack, client, apiCommand.getId());
                if (isExited(pollingResult)) {
                    throw new CancellationException("Cluster was terminated while waiting for host decommission");
                } else if (isTimeout(pollingResult)) {
                    throw new CloudbreakServiceException("Timeout while Cloudera Manager decommissioned host.");
                }
                deleteHost(stack, hostMetadata, client);
                deletedHosts.put(hostMetadata.getHostName(), hostMetadata);
            } catch (ApiException e) {
                LOGGER.error("Failed to decommission host: {}", hostMetadata.getHostName(), e);
                throw new CloudbreakServiceException(e.getMessage(), e);
            }
        };
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

    public void deleteHost(Stack stack, HostMetadata data, ApiClient client) {
        LOGGER.debug("Deleting host: [{}]", data.getHostName());
        deleteRolesFromHost(stack, data, client);
        deleteHostFromClouderaManager(stack, data, client);
        deleteUnusedCredentialsFromCluster(stack, data, client);
    }

    private void deleteUnusedCredentialsFromCluster(Stack stack, HostMetadata data, ApiClient client) {
        LOGGER.debug("Deleting unused credentials");
        ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);
        try {
            ApiCommand command = clouderaManagerResourceApi.deleteCredentialsCommand("unused");
            clouderaManagerPollingServiceProvider.startPollingCmKerberosJob(stack, client, command.getId());
        } catch (ApiException e) {
            LOGGER.error("Failed to delete credentials of host: {}", data.getHostName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteHostFromClouderaManager(Stack stack, HostMetadata data, ApiClient client) {
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
        try {
            ApiHostRefList hostRefList = clustersResourceApi.listHosts(stack.getName());
            Optional<ApiHostRef> hostRefOptional = hostRefList.getItems().stream()
                    .filter(host -> data.getHostName().equals(host.getHostname()))
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
            LOGGER.error("Failed to delete host: {}", data.getHostName(), e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void deleteRolesFromHost(Stack stack, HostMetadata data, ApiClient client) {
        LOGGER.debug("Deleting roles from host: [{}]", data.getHostName());
        ServicesResourceApi servicesResourceApi = new ServicesResourceApi(client);
        RolesResourceApi rolesResourceApi = new RolesResourceApi(client);

        String filter = "hostname==" + data.getHostName();

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
