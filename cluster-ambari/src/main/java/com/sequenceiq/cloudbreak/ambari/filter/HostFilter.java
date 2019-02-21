package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public interface HostFilter {

    List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts, Set<InstanceMetaData> instanceMetaDatasInStack)
            throws HostFilterException;
}
