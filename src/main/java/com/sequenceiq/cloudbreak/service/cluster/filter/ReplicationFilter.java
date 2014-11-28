package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam;

@Component
public class ReplicationFilter implements AmbariHostFilter {

    @Override
    public List<HostMetadata> filter(Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException {
        List<HostMetadata> result = new ArrayList<>(hosts);
        try {
            String replication = config.get(ConfigParam.DFS_REPLICATION.key());
            int replicationFactor = Integer.parseInt(replication);
            if (result.size() <= replicationFactor) {
                result.clear();
            }
        } catch (Exception e) {
            throw new HostFilterException("Cannot check the dfs replication size", e);
        }
        return result;
    }

}
