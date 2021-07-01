package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class AwsLoadBalancerMetadataCollector {

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    public Map<String, Object> getParameters(AuthenticatedContext ac, LoadBalancer loadBalancer, AwsLoadBalancerScheme scheme) {
        String region = ac.getCloudContext().getLocation().getRegion().value();
        String cFStackName = cloudFormationStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        AmazonCloudFormationClient cfRetryClient = awsClient.createCloudFormationClient(credentialView, region);
        ListStackResourcesResult result = cfRetryClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));

        Map<String, Object> parameters = parseTargetGroupCloudParams(scheme, result.getStackResourceSummaries());
        parameters.put(AwsLoadBalancerMetadataView.LOADBALANCER_ARN, loadBalancer.getLoadBalancerArn());
        return parameters;
    }

    /**
     * Parses the stack resource summary for the cloudformation stack and pulls out all listener and target group ARNs
     * associated with a particular load balancer, and creates AwsTargetGroupMetadata objects using the provided
     * resources summaries.
     * @param scheme The scheme of the load balancer being processed, either INTERNAL or INTERNET-FACING
     * @param summaries The list of resource summaries from cloud formation that contain both the logical resource id
     *                  (the name), and the physical resource id (ARN).
     * @return A list of metadata pulled from the resource summaries.
     */
    private Map<String, Object> parseTargetGroupCloudParams(AwsLoadBalancerScheme scheme, List<StackResourceSummary> summaries) {
        // Listeners and target groups have a naming convention of 'prefix + port + LB scheme'. Here we're pulling out
        // port information for the load balancer via its associated listeners, and will use that list to construct the
        // names of all listeners and target groups associated with the LB.
        List<Integer> ports = summaries.stream()
            .filter(summary -> summary.getLogicalResourceId().startsWith(AwsListener.LISTENER_NAME_PREFIX))
            .filter(summary -> summary.getLogicalResourceId().endsWith(scheme.resourceName()))
            .map(summary -> getPortFromListenerName(summary.getLogicalResourceId(), scheme))
            .collect(Collectors.toList());

        Map<String, Object> targetGroupParameters = new HashMap<>();
        // Each configured port should have a single listener and a single target group associated with it.
        // Build the appropriate names for each resource, and use that to pull out the physical resource id,
        // which in this case is the resource ARN.
        ports.forEach(port -> {
            String listenerArn = summaries.stream()
                .filter(summary -> AwsListener.getListenerName(port, scheme).equals(summary.getLogicalResourceId()))
                .map(StackResourceSummary::getPhysicalResourceId)
                .findFirst().orElse(null);
            String targetGroupArn = summaries.stream()
                .filter(summary -> AwsTargetGroup.getTargetGroupName(port, scheme).equals(summary.getLogicalResourceId()))
                .map(StackResourceSummary::getPhysicalResourceId)
                .findFirst().orElse(null);

            targetGroupParameters.put(AwsLoadBalancerMetadataView.getTargetGroupParam(port), targetGroupArn);
            targetGroupParameters.put(AwsLoadBalancerMetadataView.getListenerParam(port), listenerArn);
        });
        return targetGroupParameters;
    }

    private Integer getPortFromListenerName(String name, AwsLoadBalancerScheme scheme) {
        return Integer.valueOf(name.replace(AwsListener.LISTENER_NAME_PREFIX, "")
            .replace(scheme.resourceName(), ""));
    }
}
