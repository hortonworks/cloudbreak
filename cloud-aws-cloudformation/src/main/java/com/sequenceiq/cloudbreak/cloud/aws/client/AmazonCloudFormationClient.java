package com.sequenceiq.cloudbreak.cloud.aws.client;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.REQUEST_EXPIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_UNAVAILABLE;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClient;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
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
    private static final Set<String> RETRIABLE_ERRORS = Set.of(INTERNAL_FAILURE, REQUEST_EXPIRED, SERVICE_UNAVAILABLE);

    private final CloudFormationClient client;

    private final Retry retry;

    public AmazonCloudFormationClient(CloudFormationClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public DescribeStacksResponse describeStacks(DescribeStacksRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeStacks(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public CreateStackResponse createStack(CreateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.createStack(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DeleteStackResponse deleteStack(DeleteStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.deleteStack(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeStackResourceResponse describeStackResource(DescribeStackResourceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeStackResource(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeStackResourcesResponse describeStackResources(DescribeStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeStackResources(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeStackEventsResponse describeStackEvents(DescribeStackEventsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeStackEvents(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public UpdateStackResponse updateStack(UpdateStackRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.updateStack(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public ValidateTemplateResponse validateTemplate(ValidateTemplateRequest request) {
        return client.validateTemplate(request);
    }

    public ListStackResourcesResponse listStackResources(ListStackResourcesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.listStackResources(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    // FIXME return actual waiter instead
    public CloudFormationWaiter waiters() {
        return client.waiter();
    }

    public GetTemplateResponse getTemplate(GetTemplateRequest getTemplateRequest) {
        return client.getTemplate(getTemplateRequest);
    }

    private RuntimeException createActionFailedExceptionIfRetriableError(AwsServiceException ex) {
        if (ex.awsErrorDetails() != null) {
            String errorCode = ex.awsErrorDetails().errorCode();
            if (RETRIABLE_ERRORS.contains(errorCode)) {
                return new ActionFailedException(ex);
            }
        }
        return ex;
    }
}
