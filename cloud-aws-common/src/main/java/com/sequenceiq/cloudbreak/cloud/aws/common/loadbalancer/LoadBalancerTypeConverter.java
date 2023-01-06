package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.GATEWAY_PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNET_FACING;

import org.springframework.stereotype.Component;

import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerSchemeEnum;
import com.sequenceiq.common.api.type.LoadBalancerType;

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
            case InternetFacing:
                return LoadBalancerType.PUBLIC;
            case Internal:
            default:
                return LoadBalancerType.PRIVATE;
        }
    }
}
