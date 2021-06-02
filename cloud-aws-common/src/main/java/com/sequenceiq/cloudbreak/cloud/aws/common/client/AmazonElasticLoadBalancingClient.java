package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthResult;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsResult;

public class AmazonElasticLoadBalancingClient extends AmazonClient {

    private final AmazonElasticLoadBalancing client;

    public AmazonElasticLoadBalancingClient(AmazonElasticLoadBalancing client) {
        this.client = client;
    }

    public DescribeLoadBalancersResult describeLoadBalancers(DescribeLoadBalancersRequest describeLoadBalancersRequest) {
        return client.describeLoadBalancers(describeLoadBalancersRequest);
    }

    public DescribeTargetHealthResult describeTargetHealth(DescribeTargetHealthRequest describeTargetHealthRequest) {
        return client.describeTargetHealth(describeTargetHealthRequest);
    }

    public RegisterTargetsResult registerTargets(RegisterTargetsRequest registerTargetsRequest) {
        return client.registerTargets(registerTargetsRequest);
    }

    public DeregisterTargetsResult deregisterTargets(DeregisterTargetsRequest deregisterTargetsRequest) {
        return client.deregisterTargets(deregisterTargetsRequest);
    }
}
