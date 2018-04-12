package com.sequenceiq.cloudbreak.service.cluster.filter;

import com.sequenceiq.cloudbreak.domain.HostMetadata;

import java.util.List;
import java.util.Map;

public interface HostFilter {

    List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException;
}
