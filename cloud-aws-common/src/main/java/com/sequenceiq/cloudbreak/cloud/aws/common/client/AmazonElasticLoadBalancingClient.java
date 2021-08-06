package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeregisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
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

    public CreateTargetGroupResult createTargetGroup(CreateTargetGroupRequest createTargetGroupRequest) {
        return client.createTargetGroup(createTargetGroupRequest);
    }

    public DeleteTargetGroupResult deleteTargetGroup(DeleteTargetGroupRequest deleteTargetGroupRequest) {
        return client.deleteTargetGroup(deleteTargetGroupRequest);
    }

    public RegisterTargetsResult registerTargets(RegisterTargetsRequest registerTargetsRequest) {
        return client.registerTargets(registerTargetsRequest);
    }

    public DeregisterTargetsResult deregisterTargets(DeregisterTargetsRequest deregisterTargetsRequest) {
        return client.deregisterTargets(deregisterTargetsRequest);
    }

    public CreateLoadBalancerResult registerLoadBalancer(CreateLoadBalancerRequest request) {
        return client.createLoadBalancer(request);
    }

    public CreateListenerResult registerListener(CreateListenerRequest request) {
        return client.createListener(request);
    }

    public DescribeListenersResult describeListeners(DescribeListenersRequest request) {
        return client.describeListeners(request);
    }

    public DeleteListenerResult deleteListener(DeleteListenerRequest deleteListenerRequest) {
        return client.deleteListener(deleteListenerRequest);
    }

    public DeleteLoadBalancerResult deleteLoadBalancer(DeleteLoadBalancerRequest deleteListenerRequest) {
        return client.deleteLoadBalancer(deleteListenerRequest);
    }

    public DescribeTargetGroupsResult describeTargetGroup(DescribeTargetGroupsRequest describeTargetGroupsRequest) {
        return client.describeTargetGroups(describeTargetGroupsRequest);
    }
}
