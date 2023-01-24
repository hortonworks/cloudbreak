package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.EXTERNAL;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.INTERNAL;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;

@Component
public class GcpLoadBalancerTypeConverter {

    public GcpLoadBalancerScheme getScheme(CloudLoadBalancer cloudLoadBalancer) {
        switch (cloudLoadBalancer.getType()) {
            case PUBLIC:
                return EXTERNAL;
            case PRIVATE:
            default:
                return INTERNAL;
        }
    }

    public GcpLoadBalancerScheme getScheme(String gcpType) {
        if (EXTERNAL.getGcpType().equals(gcpType)) {
            return EXTERNAL;
        }
        return INTERNAL;
    }
}
