package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
@Scope("prototype")
public class ClouderaManagerClusterDecomissionService implements ClusterDecomissionService {

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerDecomissioner clouderaManagerDecomissioner;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerClusterDecomissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void verifyNodesAreRemovable(Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, int defaultRootVolumeSize,
            List<InstanceMetaData> notDeletedNodes) {
        clouderaManagerDecomissioner.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, defaultRootVolumeSize, client, notDeletedNodes);
    }

    @Override
    public Set<String> collectDownscaleCandidates(@Nonnull HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        return clouderaManagerDecomissioner.collectDownscaleCandidates(client, stack, hostGroup, scalingAdjustment, defaultRootVolumeSize,
                instanceMetaDatasInStack);
    }

    @Override
    public Map<String, HostMetadata> collectHostsToRemove(@Nonnull HostGroup hostGroup, Set<String> hostNames) {
        return clouderaManagerDecomissioner.collectHostsToRemove(stack, hostGroup, hostNames, client);
    }

    @Override
    public Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) {
        return clouderaManagerDecomissioner.decommissionNodes(stack, hostsToRemove, client);
    }

    @Override
    public boolean deleteHostFromCluster(HostMetadata data) {
        return clouderaManagerDecomissioner.deleteHost(stack, data, client);
    }
}