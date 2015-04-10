package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StackToMapConverter extends AbstractConversionServiceAwareConverter<Stack, Map> {

    @Override
    public Map convert(Stack stack) {
        Map<String, Object> stackStatus = new HashMap<>();
        stackStatus.put("id", stack.getId());
        stackStatus.put("status", stack.getStatus().name());
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            stackStatus.put("clusterStatus", cluster.getStatus().name());
        }
        return stackStatus;
    }
}
