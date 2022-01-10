package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public interface ClusterCommissionService {

    Map<String, InstanceMetaData> collectHostsToCommission(@Nonnull HostGroup hostGroup, Set<String> hostNames);

    Set<String> recommissionClusterNodes(Map<String, InstanceMetaData> hostsToRecommission);

    void recommissionHosts(List<String> hostsToRecommission);
}
