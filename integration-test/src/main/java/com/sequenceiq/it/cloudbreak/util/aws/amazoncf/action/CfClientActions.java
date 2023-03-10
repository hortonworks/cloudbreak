package com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action;

import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_IN_PROGRESS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.client.CfClient;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;

@Component
public class CfClientActions {
    private static final String AWS_EC_2_LAUNCH_TEMPLATE = "AWS::EC2::LaunchTemplate";

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    private static final Set<StackStatus> DO_NOT_LIST_CLOUDFORMATIONS_STATUSES = Set.of(DELETE_COMPLETE, DELETE_IN_PROGRESS);

    @Inject
    private CfClient cfClient;

    public List<StackSummary> listCfStacksByName(String name) {
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            return client.listStacks().stackSummaries()
                    .stream().filter(stack -> stack.stackName().startsWith(name)
                            && !DO_NOT_LIST_CLOUDFORMATIONS_STATUSES.contains(stack.stackStatus())).collect(Collectors.toList());
        }
    }

    public List<Stack> listCfStacksByTagsEnvironmentCrn(String crn) {
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            return client.describeStacks().stacks().stream().filter(
                    stack -> stack.tags().stream().anyMatch(tag -> tag.key().equals(CLOUDERA_ENVIRONMENT_RESOURCE_NAME) && tag.value().equals(crn)
                            && !DO_NOT_LIST_CLOUDFORMATIONS_STATUSES.contains(stack.stackStatus()))
            ).collect(Collectors.toList());
        }
    }

    public List<StackResourceSummary> getLaunchTemplatesToStack(String name) {
        ListStackResourcesResponse response;
        try (CloudFormationClient client = cfClient.buildCfClient()) {
            List<StackSummary> list = listCfStacksByName(name);
            if (list.size() == 0) {
                return null;
            }
            DescribeStacksResponse desc = client.describeStacks(DescribeStacksRequest.builder().stackName(list.get(0).stackName()).build());

            response = client.listStackResources(ListStackResourcesRequest.builder()
                    .stackName(desc.stacks().get(0).stackName()).build());
        }
        return response.stackResourceSummaries().stream()
                .filter(resource -> AWS_EC_2_LAUNCH_TEMPLATE.equals(resource.resourceType())).collect(Collectors.toList());
    }
}
