package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client.CfClient;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;

@Component
public class CfClientActions {
    private static final String AWS_EC_2_LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    private static final Set<StackStatus> DO_NOT_LIST_CLOUDFORMATION_STATUSES = Set.of(
            StackStatus.UNKNOWN_TO_SDK_VERSION, StackStatus.DELETE_COMPLETE, StackStatus.DELETE_IN_PROGRESS);

    private static final Set<StackStatus> LIST_CLOUDFORMATION_STATUSES = Arrays.stream(StackStatus.values())
            .filter(stackStatus -> !DO_NOT_LIST_CLOUDFORMATION_STATUSES.contains(stackStatus))
            .collect(Collectors.toSet());

    @Inject
    private CfClient cfClient;

    public List<StackSummary> listCfStacksByName(String name) {
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            String nextToken = null;
            List<StackSummary> stacks = new LinkedList<>();
            do {
                ListStacksRequest request = ListStacksRequest.builder()
                        .stackStatusFilters(LIST_CLOUDFORMATION_STATUSES)
                        .nextToken(nextToken)
                        .build();
                ListStacksResponse listStacksResult = client.listStacks(request);
                nextToken = listStacksResult.nextToken();
                List<StackSummary> stackSummaryList = listStacksResult.stackSummaries().stream()
                        .filter(stack -> stack.stackName().startsWith(name))
                        .collect(Collectors.toList());
                stacks.addAll(stackSummaryList);
            } while (nextToken != null);
            return stacks;
        }
    }

    public List<Stack> listCfStacksByTagsEnvironmentCrn(String crn) {
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            return client.describeStacks().stacks().stream().filter(
                    stack ->
                            stack.tags().stream().anyMatch(tag -> tag.key().equals(CLOUDERA_ENVIRONMENT_RESOURCE_NAME) && tag.value().equals(crn)
                                    && LIST_CLOUDFORMATION_STATUSES.contains(stack.stackStatus()))
            ).collect(Collectors.toList());
        }
    }

    public List<StackResourceSummary> getLaunchTemplatesToStack(String name) {
        List<StackSummary> list = listCfStacksByName(name);
        if (list.size() != 1) {
            Set<String> stackNames = list.stream().map(StackSummary::stackName).collect(Collectors.toSet());
            throw new TestFailException(String.format("Expected exactly one CF stack by name %s but found %s", name, stackNames));
        }
        String stackName = list.get(0).stackName();
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            ListStackResourcesResponse resources = client.listStackResources(ListStackResourcesRequest.builder().stackName(stackName).build());
            return resources.stackResourceSummaries().stream()
                    .filter(resource -> AWS_EC_2_LAUNCH_TEMPLATE.equals(resource.resourceType()))
                    .collect(Collectors.toList());
        }
    }
}
