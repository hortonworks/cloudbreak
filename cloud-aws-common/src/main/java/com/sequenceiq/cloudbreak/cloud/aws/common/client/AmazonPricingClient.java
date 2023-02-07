package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonPricingClient extends AmazonClient {

    private final AWSPricing client;

    private final Retry retry;

    public AmazonPricingClient(AWSPricing client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public GetProductsResult getProducts(GetProductsRequest request) {
        return retry.testWith1SecDelayMax5Times(() -> client.getProducts(request));
    }
}
