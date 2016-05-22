package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class StackToStatusConverter extends AbstractConversionServiceAwareConverter<Stack, Map> {

    @Override
    public Map convert(Stack source) {
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
