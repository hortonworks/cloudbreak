package com.sequenceiq.cloudbreak.cloud.aws.client;

import java.util.function.Supplier;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;

public class AmazonCloudFormationRetryClient {

    private final AmazonCloudFormationClient client;

    private final Retry retry;

    public AmazonCloudFormationRetryClient(AmazonCloudFormationClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeStacksResult describeStacks(DescribeStacksRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeStacks(request)));
    }

    private <T> T mapThrottlingError(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SdkClientException e) {
            if (e.getMessage().contains("Rate exceeded")) {
                throw new ActionWentFailException(e.getMessage());
            }
            throw e;
        }
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
}
