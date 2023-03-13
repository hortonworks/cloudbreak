package com.sequenceiq.cloudbreak.cloud.aws.common.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    public void testValidateCloudStorageType(boolean fipsEnabled, boolean govCloud, boolean shouldRunEndpointConfig, Optional<String> expected) {
        ReflectionTestUtils.setField(underTest, "fipsEnabled", fipsEnabled);

        if (shouldRunEndpointConfig) {
            when(awsServiceEndpointProvider.service(SERVICE, govCloud)).thenReturn(SERVICE);
            when(awsRegionEndpointProvider.region(SERVICE, REGION, govCloud)).thenReturn(REGION);
        }

        Optional<String> actual = underTest.setupFipsEndpointIfNecessary(SERVICE, REGION, govCloud);

        assertEquals(expected, actual);
    }

    static Object[][] scenariosToGenerateEndpointConfig() {
        return new Object[][]{
                { true,  true,  true, Optional.of("https://service.region.amazonaws.com")},
                { true,  false, false, Optional.empty()},
                { false, true,  false, Optional.empty()},
                { false, false, false, Optional.empty()},
        };
    }

}