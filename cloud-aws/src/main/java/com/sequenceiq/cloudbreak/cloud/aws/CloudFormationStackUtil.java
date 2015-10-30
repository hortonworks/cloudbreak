package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_MAX_AWS_RESOURCE_NAME_LENGTH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class CloudFormationStackUtil {


    @Value("${cb.max.aws.resource.name.length:" + CB_MAX_AWS_RESOURCE_NAME_LENGTH + "}")
    private int maxResourceNameLength;

    @Inject
    private AwsClient awsClient;

    public String getAutoscalingGroupName(AuthenticatedContext ac, String instanceGroup, String region) {
        AmazonCloudFormationClient amazonCfClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
        return getAutoscalingGroupName(ac, amazonCfClient, instanceGroup);
    }

    public String getAutoscalingGroupName(AuthenticatedContext ac, AmazonCloudFormationClient amazonCFClient, String instanceGroup) {
        String cFStackName = getCfStackName(ac);
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(cFStackName)
                .withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", ""))));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public String getCfStackName(AuthenticatedContext ac) {
        return String.format("%s-%s", new String(Splitter.fixedLength(maxResourceNameLength - (ac.getCloudContext().getId().toString().length() + 1))
                .splitToList(ac.getCloudContext().getName()).get(0)), ac.getCloudContext().getId());
    }

    public List<String> getInstanceIds(AmazonAutoScalingClient amazonASClient, String asGroupName) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName));
        List<String> instanceIds = new ArrayList<>();
        if (describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances() != null) {
            for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
        }
        return instanceIds;
    }

    public DescribeInstancesRequest createDescribeInstancesRequest(Collection<String> instanceIds) {
        return new DescribeInstancesRequest().withInstanceIds(instanceIds);
    }

}