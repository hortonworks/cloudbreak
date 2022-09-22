package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClient;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateRequest;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateResponse;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;

public class AmazonCloudFormationClient extends AmazonClient {

    private final CloudFormationClient client;

    private final Retry retry;

    public AmazonCloudFormationClient(CloudFormationClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeStacksResponse describeStacks(DescribeStacksRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStacks(request));
    }

    public CreateStackResponse createStack(CreateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.createStack(request));
    }

    public DeleteStackResponse deleteStack(DeleteStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteStack(request));
    }

    public DescribeStackResourceResponse describeStackResource(DescribeStackResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackResource(request));
    }

    public DescribeStackResourcesResponse describeStackResources(DescribeStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackResources(request));
    }

    public DescribeStackEventsResponse describeStackEvents(DescribeStackEventsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackEvents(request));
    }

    public UpdateStackResponse updateStack(UpdateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.updateStack(request));
    }

    public ValidateTemplateResponse validateTemplate(ValidateTemplateRequest request) {
        return client.validateTemplate(request);
    }

    public ListStackResourcesResponse listStackResources(ListStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.listStackResources(request));
    }

    // FIXME return actual waiter instead
    public CloudFormationWaiter waiters() {
        return client.waiter();
    }

    public GetTemplateResponse getTemplate(GetTemplateRequest getTemplateRequest) {
        return client.getTemplate(getTemplateRequest);
    }
}
