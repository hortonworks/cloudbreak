package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.HostMetadata;

public interface AmbariHostFilter {

    List<HostMetadata> filter(Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException;
}
