package com.sequenceiq.cloudbreak.cm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
@Scope("prototype")
public class ClouderaManagerClusterDecomissionService implements ClusterDecomissionService {

    @Override
    public void verifyNodesAreRemovable(Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, int defaultRootVolumeSize,
            List<InstanceMetaData> notDeletedNodes) {

    }

    @Override
    public Set<String> collectDownscaleCandidates(HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) {
        return Collections.emptySet();
    }

    @Override
    public Map<String, HostMetadata> collectHostsToRemove(HostGroup hostGroup, Set<String> hostNames) {
        return Collections.emptyMap();
    }

    @Override
    public Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) {
        return Collections.emptySet();
    }

    @Override
    public boolean deleteHostFromCluster(HostMetadata data) {
        return false;
    }
}
