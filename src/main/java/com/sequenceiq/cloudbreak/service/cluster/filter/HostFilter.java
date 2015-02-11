package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.HostMetadata;

public interface HostFilter {

    List<HostMetadata> filter(long stackId, Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException;
}
