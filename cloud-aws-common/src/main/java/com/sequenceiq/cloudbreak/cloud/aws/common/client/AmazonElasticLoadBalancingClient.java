package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteListenerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsResponse;

public class AmazonElasticLoadBalancingClient extends AmazonClient {

    private final ElasticLoadBalancingV2Client client;

    public AmazonElasticLoadBalancingClient(ElasticLoadBalancingV2Client client) {
        this.client = client;
    }

    public DescribeLoadBalancersResponse describeLoadBalancers(DescribeLoadBalancersRequest describeLoadBalancersRequest) {
        return client.describeLoadBalancers(describeLoadBalancersRequest);
    }

    public DescribeTargetHealthResponse describeTargetHealth(DescribeTargetHealthRequest describeTargetHealthRequest) {
        return client.describeTargetHealth(describeTargetHealthRequest);
    }

    public CreateTargetGroupResponse createTargetGroup(CreateTargetGroupRequest createTargetGroupRequest) {
        return client.createTargetGroup(createTargetGroupRequest);
    }

    public DeleteTargetGroupResponse deleteTargetGroup(DeleteTargetGroupRequest deleteTargetGroupRequest) {
        return client.deleteTargetGroup(deleteTargetGroupRequest);
    }

    public RegisterTargetsResponse registerTargets(RegisterTargetsRequest registerTargetsRequest) {
        return client.registerTargets(registerTargetsRequest);
    }

    public DeregisterTargetsResponse deregisterTargets(DeregisterTargetsRequest deregisterTargetsRequest) {
        return client.deregisterTargets(deregisterTargetsRequest);
    }

    public CreateLoadBalancerResponse registerLoadBalancer(CreateLoadBalancerRequest request) {
        return client.createLoadBalancer(request);
    }

    public ModifyLoadBalancerAttributesResponse modifyLoadBalancerAttributes(ModifyLoadBalancerAttributesRequest request) {
        return client.modifyLoadBalancerAttributes(request);
    }

    public CreateListenerResponse registerListener(CreateListenerRequest request) {
        return client.createListener(request);
    }

    public DescribeListenersResponse describeListeners(DescribeListenersRequest request) {
        return client.describeListeners(request);
    }

    public DeleteListenerResponse deleteListener(DeleteListenerRequest deleteListenerRequest) {
        return client.deleteListener(deleteListenerRequest);
    }

    public DeleteLoadBalancerResponse deleteLoadBalancer(DeleteLoadBalancerRequest deleteListenerRequest) {
        return client.deleteLoadBalancer(deleteListenerRequest);
    }

    public DescribeTargetGroupsResponse describeTargetGroup(DescribeTargetGroupsRequest describeTargetGroupsRequest) {
        return client.describeTargetGroups(describeTargetGroupsRequest);
    }
}
