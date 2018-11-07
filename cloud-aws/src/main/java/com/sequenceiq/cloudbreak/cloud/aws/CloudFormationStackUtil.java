package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Service
public class CloudFormationStackUtil {

    private static final String INSTANCE_LIFECYCLE_IN_SERVICE = "InService";

    @Value("${cb.max.aws.resource.name.length:}")
    private int maxResourceNameLength;

    @Inject
    private AwsClient awsClient;

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String getAutoscalingGroupName(AuthenticatedContext ac, String instanceGroup, String region) {
        AmazonCloudFormationRetryClient amazonCfClient = awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()), region);
        return getAutoscalingGroupName(ac, amazonCfClient, instanceGroup);
    }

    @Retryable(
            value = SdkClientException.class,
            maxAttempts = 15,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String getAutoscalingGroupName(AuthenticatedContext ac, AmazonCloudFormationRetryClient amazonCFClient, String instanceGroup) {
        String cFStackName = getCfStackName(ac);
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(cFStackName)
                .withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", ""))));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public String getCfStackName(AuthenticatedContext ac) {
        return String.format("%s-%s", Splitter.fixedLength(maxResourceNameLength - (ac.getCloudContext().getId().toString().length() + 1))
                .splitToList(ac.getCloudContext().getName()).get(0), ac.getCloudContext().getId());
    }

    public Map<Group, List<String>> getInstanceIdsByGroups(AmazonAutoScalingRetryClient amazonASClient, Map<String, Group> groupNameMapping) {
        DescribeAutoScalingGroupsResult result = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupNameMapping.keySet()));
        return result.getAutoScalingGroups().stream()
                .collect(Collectors.toMap(
                        ag -> groupNameMapping.get(ag.getAutoScalingGroupName()),
                        ag -> ag.getInstances().stream()
                                .filter(instance -> INSTANCE_LIFECYCLE_IN_SERVICE.equals(instance.getLifecycleState()))
                                .map(Instance::getInstanceId)
                                .collect(Collectors.toList())));
    }

    public List<String> getInstanceIds(AmazonAutoScalingRetryClient amazonASClient, String asGroupName) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName));
        List<String> instanceIds = new ArrayList<>();
        if (!describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()
                && describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances() != null) {
            for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
                if (INSTANCE_LIFECYCLE_IN_SERVICE.equals(instance.getLifecycleState())) {
                    instanceIds.add(instance.getInstanceId());
                }
            }
        }
        return instanceIds;
    }

    public DescribeInstancesRequest createDescribeInstancesRequest(Collection<String> instanceIds) {
        return new DescribeInstancesRequest().withInstanceIds(instanceIds);
    }

}