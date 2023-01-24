package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.EXTERNAL;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.GATEWAY_INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.INTERNAL;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.GcpMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;

@Component
public class GcpLoadBalancerTypeConverter {

    public GcpLoadBalancerScheme getScheme(CloudLoadBalancer cloudLoadBalancer) {
        switch (cloudLoadBalancer.getType()) {
            case PUBLIC:
                return EXTERNAL;
            case GATEWAY_PRIVATE:
                return GATEWAY_INTERNAL;
            case PRIVATE:
            default:
                return INTERNAL;
        }
    }

    /**
     * Converts GCP API LB scheme to CB internal GCP LB scheme.
     * The latter contains an extra GATEWAY_PRIVATE which is determined in the {@link GcpMetadataCollector#collectLoadBalancer}
     * based on CloudResource metadata.
     *
     * @param gcpType GCP Load Balancer Scheme (EXTERNAL or INTERNAL) from GCP API response
     * @return CB-internal GCP-specific LB scheme
     */
    public GcpLoadBalancerScheme getScheme(String gcpType) {
        if (EXTERNAL.getGcpType().equals(gcpType)) {
            return EXTERNAL;
        }
        return INTERNAL;
    }
}
