package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@ExtendWith(MockitoExtension.class)
class AwsEndpointProviderTest {

    private static final String REGION = "region";

    private static final String SERVICE = "service";

    @Mock
    private AwsRegionEndpointProvider awsRegionEndpointProvider;

    @Mock
    private AwsServiceEndpointProvider awsServiceEndpointProvider;

    @InjectMocks
    private AwsEndpointProvider underTest;

    @ParameterizedTest(name = "With fipsEnabled={0}, govCloud: {1}, shouldRunEndpointConfig: {2}, expected: {3}")
    @MethodSource("scenariosToGenerateEndpointConfig")
    public void testValidateCloudStorageType(boolean fipsEnabled, boolean govCloud, boolean shouldRunEndpointConfig, String expected) {
        ReflectionTestUtils.setField(underTest, "fipsEnabled", fipsEnabled);
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard()
                .withForceGlobalBucketAccessEnabled(Boolean.TRUE);

        if (shouldRunEndpointConfig) {
            when(awsServiceEndpointProvider.service(SERVICE, govCloud)).thenReturn(SERVICE);
            when(awsRegionEndpointProvider.region(SERVICE, REGION, govCloud)).thenReturn(REGION);
        }

        underTest.setupEndpoint(clientBuilder, SERVICE, REGION, govCloud);

        if (shouldRunEndpointConfig) {
            AwsClientBuilder.EndpointConfiguration endpoint = clientBuilder.getEndpoint();
            assertEquals(expected, endpoint.getServiceEndpoint());
        } else {
            String region = clientBuilder.getRegion();
            assertEquals(expected, region);
        }
    }

    static Object[][] scenariosToGenerateEndpointConfig() {
        return new Object[][]{
                { true,  true,  true, "https://service.region.amazonaws.com"},
                { true,  false, false, "region"},
                { false, true,  false, "region"},
                { false, false, false, "region"},
        };
    }

}