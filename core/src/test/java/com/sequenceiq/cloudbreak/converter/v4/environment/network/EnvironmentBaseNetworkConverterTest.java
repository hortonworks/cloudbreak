package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBaseNetworkConverterTest {

    @InjectMocks
    private TestEnvironmentBaseNetworkConverter underTest;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Test
    public void testConvertToLegacyNetworkWhenSubnetNotFound() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.convertToLegacyNetwork(source, "eu-west-1a"));
        assertEquals(badRequestException.getMessage(), "No subnet for the given availability zone: eu-west-1a");
    }

    @Test
    public void testConvertToLegacyNetworkWhenSubnetFound() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("eu-west-1a")));
        Network network = underTest.convertToLegacyNetwork(source, "eu-west-1a");
        assertEquals(network.getAttributes().getValue("subnetId"), "eu-west-1");
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
    }

    private static class TestEnvironmentBaseNetworkConverter extends EnvironmentBaseNetworkConverter {

        @Override
        Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
            return Collections.emptyMap();
        }

        @Override
        public CloudPlatform getCloudPlatform() {
            return CloudPlatform.AWS;
        }
    }
}
