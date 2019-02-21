package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterDecomissionService {
    void verifyNodesAreRemovable(Multimap<Long, HostMetadata> hostGroupWithInstances, Set<HostGroup> hostGroups, int defaultRootVolumeSize,
            List<InstanceMetaData> notDeletedNodes);

    Set<String> collectDownscaleCandidates(HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) throws CloudbreakException;

    Map<String, HostMetadata> collectHostsToRemove(HostGroup hostGroup, Set<String> hostNames);

    Set<HostMetadata> decommissionClusterNodes(Map<String, HostMetadata> hostsToRemove) throws CloudbreakException;

    boolean deleteHostFromCluster(HostMetadata data);
}
