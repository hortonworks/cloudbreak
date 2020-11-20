package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonCloudFormationRetryClient extends AmazonRetryClient {

    private final AmazonCloudFormationClient client;

    private final Retry retry;

    public AmazonCloudFormationRetryClient(AmazonCloudFormationClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeStacksResult describeStacks(DescribeStacksRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeStacks(request)));
    }

    public CreateStackResult createStack(CreateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.createStack(request)));
    }

    public DeleteStackResult deleteStack(DeleteStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.deleteStack(request)));
    }

    public DescribeStackResourceResult describeStackResource(DescribeStackResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeStackResource(request)));
    }

    public DescribeStackResourcesResult describeStackResources(DescribeStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeStackResources(request)));
    }

    public UpdateStackResult updateStack(UpdateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.updateStack(request)));
    }

    public ListStackResourcesResult listStackResources(ListStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.listStackResources(request)));
    }
}
