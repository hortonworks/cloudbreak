package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;

public class StackToStatusConverter extends AbstractConversionServiceAwareConverter<Stack, Map<String, Object>> {
    @Override
    public Map<String, Object> convert(Stack source) {
        Map<String, Object> stackStatus = new HashMap<>();
        stackStatus.put("id", source.getId());
        stackStatus.put("status", source.getStatus().name());
        Cluster cluster = source.getCluster();
        if (cluster != null) {
            stackStatus.put("clusterStatus", cluster.getStatus().name());
        }
        return stackStatus;
    }
}
