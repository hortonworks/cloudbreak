package com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer;

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
            case PRIVATE:
            default:
                return INTERNAL;
        }
    }

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
