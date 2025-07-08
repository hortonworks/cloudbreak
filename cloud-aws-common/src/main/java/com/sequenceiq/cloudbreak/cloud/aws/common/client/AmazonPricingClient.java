package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_ERROR_EXCEPTION;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.REQUEST_EXPIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_UNAVAILABLE;

import java.util.Set;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

public class AmazonPricingClient extends AmazonClient {
    private static final Set<String> RETRIABLE_ERRORS = Set.of(INTERNAL_FAILURE, INTERNAL_ERROR_EXCEPTION, REQUEST_EXPIRED, SERVICE_UNAVAILABLE);

    private final PricingClient client;

    private final Retry retry;

    public AmazonPricingClient(PricingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public GetProductsResponse getProducts(GetProductsRequest request) {
        return retry.testWith1SecDelayMax5Times(() -> {
            try {
                return client.getProducts(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
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
