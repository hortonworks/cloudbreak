package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonElbV2RetryClient extends AmazonRetryClient {

    private final AmazonElasticLoadBalancingClient client;

    private final Retry retry;

    public AmazonElbV2RetryClient(AmazonElasticLoadBalancingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeLoadBalancersResult describeLoadBalancer(DescribeLoadBalancersRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeLoadBalancers(request)));
    }

    public DescribeTargetGroupsResult describeLoadBalancer(DescribeTargetGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeTargetGroups(request)));
    }
}
