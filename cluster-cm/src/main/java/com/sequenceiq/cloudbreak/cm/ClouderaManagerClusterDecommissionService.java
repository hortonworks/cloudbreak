package com.sequenceiq.cloudbreak.cm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterDecommissionService implements ClusterDecomissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterDecommissionService.class);

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerDecomissioner clouderaManagerDecomissioner;

    @Inject
    private ApplicationContext applicationContext;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    private ApiClient v31Client;

    private ApiClient v45Client;

    private ApiClient v51Client;

    private ApiClient v53Client;

    public ClouderaManagerClusterDecommissionService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            v31Client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
        try {
            v45Client = clouderaManagerApiClientProvider.getV45Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.warn("Client init failed for V45 client!");
        }
        try {
            v51Client = clouderaManagerApiClientProvider.getV51Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.warn("Client init failed for V51 client!");
        }

        try {
            v53Client = clouderaManagerApiClientProvider.getV53Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.warn("Client init failed for V53 client!");
        }
    }

    @Override
    public void verifyNodesAreRemovable(StackDtoDelegate stack, Collection<InstanceMetadataView> removableInstances) {
        clouderaManagerDecomissioner.verifyNodesAreRemovable(stack, removableInstances, v31Client);
    }

    @Override
    public Set<InstanceMetadataView> collectDownscaleCandidates(@Nonnull String hostGroupName, Integer scalingAdjustment,
            Set<InstanceMetadataView> instanceMetaDatasInStack) {
        return clouderaManagerDecomissioner.collectDownscaleCandidates(v31Client, stack, hostGroupName, scalingAdjustment, instanceMetaDatasInStack);
    }

    @Override
    public Map<String, InstanceMetadataView> collectHostsToRemove(@Nonnull String hostGroupName, Set<String> hostNames) {
        return clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroupName, hostNames, v31Client);
    }

    @Override
    public Set<String> decommissionClusterNodes(Map<String, InstanceMetadataView> hostsToRemove) {
        return clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, v31Client);
    }

    @Override
    public Set<String> decommissionClusterNodesStopStart(Map<String, InstanceMetadataView> hostsToRemove, long pollingTimeout) {
        return clouderaManagerDecomissioner.decommissionNodesStopStart(stack, hostsToRemove, v31Client, pollingTimeout);
    }

    @Override
    public void enterMaintenanceMode(Set<String> hostFqdnList) {
        clouderaManagerDecomissioner.enterMaintenanceMode(hostFqdnList, v31Client);
    }

    @Override
    public void removeManagementServices() {
        clouderaManagerDecomissioner.stopAndRemoveMgmtService(stack, v31Client);
    }

    @Override
    public void deleteHostFromCluster(InstanceMetadataView data) {
        clouderaManagerDecomissioner.deleteHost(stack, data, v31Client);
    }

    @Override
    public void removeHostsFromCluster(List<InstanceMetadataView> hosts) throws ClusterClientInitException {
        if (v45Client != null) {
            clouderaManagerDecomissioner.removeHostsFromCluster(stack, hosts, v45Client);
        } else {
            LOGGER.error("V45 client is not initialized, bulk host removal is not supported");
            throw new ClusterClientInitException("V45 client is not initialized, bulk host removal is not supported");
        }
    }

    @Override
    public void deleteUnusedCredentialsFromCluster() {
        clouderaManagerDecomissioner.deleteUnusedCredentialsFromCluster(stack, v31Client);
    }

    @Override
    public void restartStaleServices(boolean forced) throws CloudbreakException {
        try {
            applicationContext.getBean(ClouderaManagerModificationService.class, stack, clientConfig)
                    .restartStaleServices(clouderaManagerApiFactory.getClustersResourceApi(v31Client), forced);
        } catch (ApiException e) {
            LOGGER.error("Couldn't restart stale services", e);
            throw new CloudbreakException("Couldn't restart stale services", e);
        }
    }

    @Override
    public void stopRolesOnHosts(Set<String> hosts, boolean stopServicesGracefully) throws CloudbreakException {
        clouderaManagerDecomissioner.stopRolesOnHosts(stack, v53Client, v51Client, hosts, stopServicesGracefully);
    }

    @Override
    public Map<String, Map<String, String>> getStatusOfComponentsForHost(String host) {
        return Map.of();
    }
}
