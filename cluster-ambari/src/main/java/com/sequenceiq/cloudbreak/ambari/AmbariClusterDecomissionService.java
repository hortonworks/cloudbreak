package com.sequenceiq.cloudbreak.ambari;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class AmbariClusterDecomissionService implements ClusterDecomissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterDecomissionService.class);

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private AmbariClient ambariClient;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    public AmbariClusterDecomissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void verifyNodesAreRemovable(Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, int defaultRootVolumeSize,
            List<InstanceMetaData> notDeletedNodes) {
        ambariDecommissioner.verifyNodesAreRemovable(stack, hostGroupWithInstances, hostGroups, defaultRootVolumeSize, ambariClient, notDeletedNodes);
    }

    @Override
    public Set<String> collectDownscaleCandidates(HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) throws CloudbreakException {
        return ambariDecommissioner.collectDownscaleCandidates(ambariClient, stack, hostGroup, scalingAdjustment, defaultRootVolumeSize,
                instanceMetaDatasInStack);
    }

    @Override
    public Map<String, HostMetadata> collectHostsToRemove(HostGroup hostGroup, Set<String> hostNames) {
        return ambariDecommissioner.collectHostsToRemove(stack, hostGroup, hostNames, ambariClient);
    }

    @Override
    public Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) throws CloudbreakException {
        return ambariDecommissioner.decommissionAmbariNodes(stack, hostsToRemove, ambariClient);
    }

    @Override
    public boolean deleteHostFromCluster(HostMetadata data) {
        return ambariDecommissioner.deleteHostFromAmbari(stack, data, ambariClient);
    }
}
