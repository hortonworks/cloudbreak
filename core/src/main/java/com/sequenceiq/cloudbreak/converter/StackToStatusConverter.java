package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StackToStatusConverter extends AbstractConversionServiceAwareConverter<Stack, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(Stack source) {
        Map<String, Object> stackStatus = new HashMap<>();
        stackStatus.put("id", source.getId());
        stackStatus.put("status", source.getStatus().name());
        stackStatus.put("statusReason", source.getStatusReason());
        Cluster cluster = source.getCluster();
        if (cluster != null) {
            stackStatus.put("clusterStatus", cluster.getStatus().name());
            stackStatus.put("clusterStatusReason", cluster.getStatusReason());
        }
        return stackStatus;
    }
}
