package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client.CfClient;

@Component
public class CfClientActions {
    private static final String AWS_EC_2_LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    private static final Set DO_NOT_LIST_CLOUDFORMATIONS_STATUSES = Set.of("DELETE_COMPLETE", "DELETE_IN_PROGRESS");

    @Inject
    private CfClient cfClient;

    public List<StackSummary> listCfStacksByName(String name) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        return client.listStacks().getStackSummaries()
                .stream().filter(stack -> stack.getStackName().startsWith(name)
                        && !DO_NOT_LIST_CLOUDFORMATIONS_STATUSES.contains(stack.getStackStatus())).collect(Collectors.toList());
    }

    public List<Stack> listCfStacksByTagsEnvironmentCrn(String crn) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        return client.describeStacks().getStacks().stream().filter(
                stack ->
                        stack.getTags().stream().anyMatch(tag -> tag.getKey().equals(CLOUDERA_ENVIRONMENT_RESOURCE_NAME) && tag.getValue().equals(crn)
                        && DO_NOT_LIST_CLOUDFORMATIONS_STATUSES.contains(stack.getStackStatus()))
        ).collect(Collectors.toList());
    }

    public List<StackResourceSummary> getLaunchTemplatesToStack(String name) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        List<StackSummary> list = listCfStacksByName(name);
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
