package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

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
