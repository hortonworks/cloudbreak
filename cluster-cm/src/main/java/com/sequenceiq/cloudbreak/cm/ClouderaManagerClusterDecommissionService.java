package com.sequenceiq.cloudbreak.cm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

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

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient v31Client;

    private ApiClient v45Client;

    public ClouderaManagerClusterDecommissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
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
    }

    @Override
    public void verifyNodesAreRemovable(Stack stack, Collection<InstanceMetaData> removableInstances) {
        clouderaManagerDecomissioner.verifyNodesAreRemovable(stack, removableInstances, v31Client);
    }

    @Override
    public Set<InstanceMetaData> collectDownscaleCandidates(@Nonnull HostGroup hostGroup, Integer scalingAdjustment,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        return clouderaManagerDecomissioner.collectDownscaleCandidates(v31Client, stack, hostGroup, scalingAdjustment, instanceMetaDatasInStack);
    }

    @Override
    public Map<String, InstanceMetaData> collectHostsToRemove(@Nonnull HostGroup hostGroup, Set<String> hostNames) {
        return clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroup, hostNames, v31Client);
    }

    @Override
    public Set<String> decommissionClusterNodes(Map<String, InstanceMetaData> hostsToRemove) {
        return clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, v31Client);
    }

    @Override
    public void enterMaintenanceMode(Stack stack, Map<String, InstanceMetaData> hostList) {
        clouderaManagerDecomissioner.enterMaintenanceMode(stack, hostList, client);
    }

    @Override
    public void removeManagementServices() {
        clouderaManagerDecomissioner.stopAndRemoveMgmtService(stack, v31Client);
    }

    @Override
    public void deleteHostFromCluster(InstanceMetaData data) {
        clouderaManagerDecomissioner.deleteHost(stack, data, v31Client);
    }

    @Override
    public void removeHostsFromCluster(List<InstanceMetaData> hosts) throws ClusterClientInitException {
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
    public Map<String, Map<String, String>> getStatusOfComponentsForHost(String host) {
        return Map.of();
    }
}
