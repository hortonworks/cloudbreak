package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.service.RetryService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;

class AmazonPricingClientTest {

    private PricingClient pricingClient;

    private AmazonPricingClient underTest;

    @BeforeEach
    void setUp() {
        pricingClient = mock(PricingClient.class);
        Retry retry = new RetryService();
        underTest = new AmazonPricingClient(pricingClient, retry);
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "InternalErrorException"})
    void testGetProductsRetriableErrorCodes(String errorCode) {
        GetProductsRequest getProductsRequest = mock(GetProductsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(pricingClient.getProducts(getProductsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.getProducts(getProductsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }
}
