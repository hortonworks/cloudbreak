package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client.CfClient;

@Component
public class CfClientActions {
    private static final String AWS_EC_2_LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    @Inject
    private CfClient cfClient;

    public List<StackSummary> listCfStacksWithName(String name) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        return client.listStacks().getStackSummaries()
                .stream().filter(stack -> stack.getStackName().startsWith(name)).collect(Collectors.toList());
    }

    public List<StackResourceSummary> getLaunchTemplatesToStack(String name) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        List<StackSummary> list = listCfStacksWithName(name);
        if (list.size() == 0) {
            return null;
        }
        DescribeStacksResult desc = client.describeStacks(new DescribeStacksRequest().withStackName(list.get(0).getStackName()));

        ListStackResourcesResult resources = client.listStackResources(new ListStackResourcesRequest()
                .withStackName(desc.getStacks().get(0).getStackName()));
        return resources.getStackResourceSummaries().stream()
                .filter(resource -> AWS_EC_2_LAUNCH_TEMPLATE.equals(resource.getResourceType())).collect(Collectors.toList());
    }
}
