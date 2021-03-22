package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonCloudFormationClient extends AmazonClient {

    private final AmazonCloudFormation client;

    private final Retry retry;

    public AmazonCloudFormationClient(AmazonCloudFormation client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeStacksResult describeStacks(DescribeStacksRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStacks(request));
    }

    public CreateStackResult createStack(CreateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.createStack(request));
    }

    public DeleteStackResult deleteStack(DeleteStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteStack(request));
    }

    public DescribeStackResourceResult describeStackResource(DescribeStackResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackResource(request));
    }

    public DescribeStackResourcesResult describeStackResources(DescribeStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackResources(request));
    }

    public DescribeStackEventsResult describeStackEvents(DescribeStackEventsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeStackEvents(request));
    }

    public UpdateStackResult updateStack(UpdateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.updateStack(request));
    }

    public ListStackResourcesResult listStackResources(ListStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.listStackResources(request));
    }

    // FIXME return actual waiter instead
    public AmazonCloudFormationWaiters waiters() {
        return client.waiters();
    }

    public GetTemplateResult getTemplate(GetTemplateRequest getTemplateRequest) {
        return client.getTemplate(getTemplateRequest);
    }
}
