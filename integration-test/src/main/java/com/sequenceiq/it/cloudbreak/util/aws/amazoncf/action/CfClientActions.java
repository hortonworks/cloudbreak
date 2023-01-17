package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client.CfClient;

@Component
public class CfClientActions {
    private static final String AWS_EC_2_LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    private static final Set<StackStatus> DO_NOT_LIST_CLOUDFORMATION_STATUSES = Set.of(StackStatus.DELETE_COMPLETE, StackStatus.DELETE_IN_PROGRESS);

    private static final Set<String> LIST_CLOUDFORMATION_STATUSES = Arrays.stream(StackStatus.values())
            .filter(stackStatus -> !DO_NOT_LIST_CLOUDFORMATION_STATUSES.contains(stackStatus))
            .map(Enum::name)
            .collect(Collectors.toSet());

    @Inject
    private CfClient cfClient;

    public List<StackSummary> listCfStacksByName(String name) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        String nextToken = null;
        List<StackSummary> stacks = new LinkedList<>();
        do {
            ListStacksRequest request = new ListStacksRequest()
                    .withStackStatusFilters(LIST_CLOUDFORMATION_STATUSES)
                    .withNextToken(nextToken);
            ListStacksResult listStacksResult = client.listStacks(request);
            nextToken = listStacksResult.getNextToken();
            List<StackSummary> stackSummaryList = listStacksResult.getStackSummaries().stream()
                    .filter(stack -> stack.getStackName().startsWith(name))
                    .collect(Collectors.toList());
            stacks.addAll(stackSummaryList);
        } while (nextToken != null);
        return stacks;
    }

    public List<Stack> listCfStacksByTagsEnvironmentCrn(String crn) {
        AmazonCloudFormation client = cfClient.buildCfClient();
        return client.describeStacks().getStacks().stream().filter(
                stack ->
                        stack.getTags().stream().anyMatch(tag -> tag.getKey().equals(CLOUDERA_ENVIRONMENT_RESOURCE_NAME) && tag.getValue().equals(crn)
                        && LIST_CLOUDFORMATION_STATUSES.contains(stack.getStackStatus()))
        ).collect(Collectors.toList());
    }

    public List<StackResourceSummary> getLaunchTemplatesToStack(String name) {
        List<StackSummary> list = listCfStacksByName(name);
        if (list.size() != 1) {
            Set<String> stackNames = list.stream().map(StackSummary::getStackName).collect(Collectors.toSet());
            throw new TestFailException(String.format("Expected exactly one CF stack by name %s but found %s", name, stackNames));
        }
        String stackName = list.get(0).getStackName();
        AmazonCloudFormation client = cfClient.buildCfClient();
        ListStackResourcesResult resources = client.listStackResources(new ListStackResourcesRequest().withStackName(stackName));
        return resources.getStackResourceSummaries().stream()
                .filter(resource -> AWS_EC_2_LAUNCH_TEMPLATE.equals(resource.getResourceType()))
                .collect(Collectors.toList());
    }
}
