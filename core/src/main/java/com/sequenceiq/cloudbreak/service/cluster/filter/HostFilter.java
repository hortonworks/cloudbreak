package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;

public interface HostFilter {

    List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException;
}
