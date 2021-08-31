package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp;

import java.util.HashMap;
import java.util.Map;

public class GcpTargetGroupConfigDb {

    private Map<Integer, GcpLoadBalancerNamesDb> portMapping = new HashMap<>();

    public Map<Integer, GcpLoadBalancerNamesDb> getPortMapping() {
        return portMapping;
    }

    public void addPortNameMapping(Integer port, GcpLoadBalancerNamesDb names) {
        portMapping.put(port, names);
    }

    @Override
    public String toString() {
        return "GcpTargetGroupConfigDb{" +
                "portMapping=" + portMapping +
                '}';
    }
}
