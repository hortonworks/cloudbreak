package com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.converter;

import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.common.api.type.LoadBalancerType;
import org.springframework.stereotype.Component;

@Component
public class LoadBalancerTypeConverter {

    public AwsLoadBalancerScheme convert(LoadBalancerType type) {
        switch (type) {
            case PUBLIC:
                return AwsLoadBalancerScheme.INTERNET_FACING;
            case PRIVATE:
            default:
                return AwsLoadBalancerScheme.INTERNAL;
        }
    }
}
