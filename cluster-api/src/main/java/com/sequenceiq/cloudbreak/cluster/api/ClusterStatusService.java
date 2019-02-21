package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;

public interface ClusterStatusService {

    Map<String, HostMetadataState> getHostStatuses();

    Map<String, String> getHostStatusesRaw();
}
