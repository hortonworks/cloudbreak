package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public interface HostFilter {

    List<InstanceMetaData> filter(long clusterId, Map<String, String> config, List<InstanceMetaData> hosts) throws HostFilterException;
}
