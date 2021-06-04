package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme.INTERNET_FACING;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.LoadBalancerType;

@Component
public class LoadBalancerTypeConverter {

    public AwsLoadBalancerScheme convert(LoadBalancerType type) {
        switch (type) {
            case PUBLIC:
                return INTERNET_FACING;
            case PRIVATE:
            default:
                return INTERNAL;
        }
    }

    public LoadBalancerType convert(String type) {
        AwsLoadBalancerScheme awsLoadBalancerScheme = AwsLoadBalancerScheme.valueOf(type.toUpperCase());
        switch (awsLoadBalancerScheme) {
            case INTERNET_FACING:
                return LoadBalancerType.PUBLIC;
            case INTERNAL:
            default:
                return LoadBalancerType.PRIVATE;
        }
    }
}
