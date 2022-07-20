package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public interface ClusterDecomissionService {
    void verifyNodesAreRemovable(StackDtoDelegate stack, Collection<InstanceMetadataView> removableInstances);

    Set<InstanceMetadataView> collectDownscaleCandidates(@Nonnull String hostGroupName, Integer scalingAdjustment,
            Set<InstanceMetadataView> instanceMetaDatasInStack) throws CloudbreakException;

    Map<String, InstanceMetadataView> collectHostsToRemove(@Nonnull String hostGroupName, Set<String> hostNames);

    Set<String> decommissionClusterNodes(Map<String, InstanceMetadataView> hostsToRemove);

    Set<String> decommissionClusterNodesStopStart(Map<String, InstanceMetadataView> hostsToRemove, long pollingTimeout);

    void enterMaintenanceMode(Set<String> hostFqdnList);

    void removeManagementServices();

    void deleteHostFromCluster(InstanceMetadataView data);

    void removeHostsFromCluster(List<InstanceMetadataView> hosts) throws ClusterClientInitException;

    void deleteUnusedCredentialsFromCluster();

    void restartStaleServices(boolean forced) throws CloudbreakException;

    Map<String, Map<String, String>> getStatusOfComponentsForHost(String host);
}
