package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

public class AmazonPricingClient extends AmazonClient {

    private final PricingClient client;

    private final Retry retry;

    public AmazonPricingClient(PricingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public GetProductsResponse getProducts(GetProductsRequest request) {
        return retry.testWith1SecDelayMax5Times(() -> client.getProducts(request));
    }
}
