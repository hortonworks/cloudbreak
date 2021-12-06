package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

public interface ClusterDecomissionService {
    void verifyNodesAreRemovable(Stack stack, Collection<InstanceMetaData> removableInstances);

    Set<InstanceMetaData> collectDownscaleCandidates(@Nonnull HostGroup hostGroup, Integer scalingAdjustment,
            Set<InstanceMetaData> instanceMetaDatasInStack) throws CloudbreakException;

    Map<String, InstanceMetaData> collectHostsToRemove(@Nonnull HostGroup hostGroup, Set<String> hostNames);

    Set<String> decommissionClusterNodes(Map<String, InstanceMetaData> hostsToRemove);

    void enterMaintenanceMode(Stack stack, Map<String, InstanceMetaData> hostList);

    void removeManagementServices();

    void deleteHostFromCluster(InstanceMetaData data);

    void removeHostsFromCluster(List<InstanceMetaData> hosts) throws ClusterClientInitException;

    void deleteUnusedCredentialsFromCluster();

    void restartStaleServices(boolean forced) throws CloudbreakException;

    Map<String, Map<String, String>> getStatusOfComponentsForHost(String host);
}
