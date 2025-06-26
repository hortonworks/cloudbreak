package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.GATEWAY_PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNET_FACING;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.LoadBalancerType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;

@Component
public class LoadBalancerTypeConverter {

    public AwsLoadBalancerScheme convert(LoadBalancerType type) {
        switch (type) {
            case PUBLIC:
                return INTERNET_FACING;
            case GATEWAY_PRIVATE:
                return GATEWAY_PRIVATE;
            case PRIVATE:
            default:
                return INTERNAL;
        }
    }

    /**
     * Converts AWS API LB scheme to CB internal LB type.
     * The latter contains an extra GATEWAY_PRIVATE which is determined in the {@link AwsNativeMetadataCollector#describeLoadBalancer}
     * based on CloudResource metadata.
     *
     * @param type AWS Load Balancer Scheme ("internet-facing" or "internal") from AWS API response
     * @return CB-internal {@link LoadBalancerType}
     */
    public LoadBalancerType convert(String type) {
        LoadBalancerSchemeEnum awsLoadBalancerScheme = LoadBalancerSchemeEnum.fromValue(type);
        switch (awsLoadBalancerScheme) {
            case INTERNET_FACING:
                return LoadBalancerType.PUBLIC;
            case INTERNAL:
            default:
                return LoadBalancerType.PRIVATE;
        }
    }

    public LoadBalancerType convert(LoadBalancerSchemeEnum type) {
        switch (type) {
            case INTERNET_FACING:
                return LoadBalancerType.PUBLIC;
            case INTERNAL:
            default:
                return LoadBalancerType.PRIVATE;
        }
    }
}
