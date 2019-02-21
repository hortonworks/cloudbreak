package com.sequenceiq.cloudbreak.cm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class ClouderaManagerClusterDecomissionService implements ClusterDecomissionService {

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

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

    }

    @Override
    public Set<String> collectDownscaleCandidates(HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) throws CloudbreakException {
        return Collections.emptySet();
    }

    @Override
    public Map<String, HostMetadata> collectHostsToRemove(HostGroup hostGroup, Set<String> hostNames) {
        return Collections.emptyMap();
    }

    @Override
    public Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) throws CloudbreakException {
        return Collections.emptySet();
    }

    @Override
    public boolean deleteHostFromCluster(HostMetadata data) {
        return false;
    }
}
