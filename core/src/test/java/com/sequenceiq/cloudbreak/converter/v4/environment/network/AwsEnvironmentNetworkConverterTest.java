package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@MockitoSettings
class AwsEnvironmentNetworkConverterTest {

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private SubnetSelector subnetSelector;

    @InjectMocks
    private AwsEnvironmentNetworkConverter converter;

    @Test
    void getAttributesForLegacyNetwork() {
        EnvironmentNetworkResponse source = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkAwsParams aws = mock(EnvironmentNetworkAwsParams.class);
        when(source.getAws()).thenReturn(aws);
        when(aws.getVpcId()).thenReturn("my_vpc_id");

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(source);

        assertEquals("my_vpc_id", result.get("vpcId"));
    }

    @Test
    void getCloudPlatform() {
        assertEquals(CloudPlatform.AWS, converter.getCloudPlatform());
    }
}