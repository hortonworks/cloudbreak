package com.sequenceiq.cloudbreak.cm;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;

@Service
@Scope("prototype")
public class ClouderaManagerClusterStatusService implements ClusterStatusService {

    @Override
    public Map<String, HostMetadataState> getHostStatuses() {
        return null;
    }

    @Override
    public Map<String, String> getHostStatusesRaw() {
        return null;
    }
}
