package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.GcpNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@ExtendWith(MockitoExtension.class)
class FreeIpaGcpNetworkProviderTest {

    @InjectMocks
    private FreeIpaGcpNetworkProvider underTest;

    @Test
    void testAvailabilityZoneCustomAvailabilityZoneIsNotSpecifiedAndGcpParamsIsNullShouldSelectEnvironmentSubnetMetaBasedAz() {
        String subnetId = "subnetId";
        String availabilityZone = "gcp-region1-zone1";
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withNetwork(NetworkDto.builder()
                        .withSubnetMetas(Map.of(subnetId,
                                new CloudSubnet.Builder()
                                        .id("id")
                                        .name("name")
                                        .availabilityZone(availabilityZone)
                                        .cidr("cidr")
                                        .build()
                        ))
                        .build())
                .build();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setSubnetId(subnetId);
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setGcp(gcpNetworkParameters);

        String result = underTest.availabilityZone(networkRequest, environmentDto);

        assertEquals(result, availabilityZone);
    }

    @Test
    void testAvailabilityZoneCustomAvailabilityZoneIsNotSpecifiedAndGcpParamsIsNotNullShouldSelectEnvironmentSubnetMetaBasedAz() {
        String subnetId = "subnetId";
        String availabilityZone = "gcp-region1-zone1";
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withNetwork(NetworkDto.builder()
                        .withSubnetMetas(Map.of(subnetId,
                                new CloudSubnet.Builder()
                                        .id("id")
                                        .name("name")
                                        .availabilityZone(availabilityZone)
                                        .cidr("cidr")
                                        .build()
                        ))
                        .withGcp(GcpParams.builder().build())
                        .build())
                .build();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setSubnetId(subnetId);
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setGcp(gcpNetworkParameters);

        String result = underTest.availabilityZone(networkRequest, environmentDto);

        assertEquals(result, availabilityZone);
    }

    @Test
    void testAvailabilityZoneCustomAvailabilityZoneIsSpecifiedShouldSelectEnvironmentSubnetMetaBasedAz() {
        String subnetId = "subnetId";
        String envAvailabilityZone = "gcp-region1-zone1";
        String customAvailabilityZone = "custom-gcp-region1-zone1";
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(subnetId,
                        new CloudSubnet.Builder()
                                .id("id")
                                .name("name")
                                .availabilityZone(envAvailabilityZone)
                                .cidr("cidr")
                                .build()
                ))
                .withGcp(GcpParams.builder()
                        .withAvailabilityZones(Set.of(customAvailabilityZone))
                        .build())
                .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withNetwork(networkDto)
                .build();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setSubnetId(subnetId);
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setGcp(gcpNetworkParameters);

        String result = underTest.availabilityZone(networkRequest, environmentDto);

        assertEquals(result, customAvailabilityZone);
    }

    @Test
    void testAvailabilityZoneMultipleAvailabilityZoneAreSpecifiedShouldSelectSubnetMetaBasedAz() {
        String subnetId = "subnetId";
        Set<String> envAvailabilityZones = Set.of("gcp-region1-zone3", "gcp-region1-zone4");
        String subnetAvailabilityZone = "gcp-region1-zone4";
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(subnetId, new CloudSubnet.Builder()
                        .id("id")
                        .name("name")
                        .availabilityZone(subnetAvailabilityZone)
                        .cidr("cidr")
                        .build()
                ))
                .withGcp(GcpParams.builder()
                        .withAvailabilityZones(envAvailabilityZones)
                        .build())
                .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withNetwork(networkDto)
                .build();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setSubnetId(subnetId);
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setGcp(gcpNetworkParameters);

        String result = underTest.availabilityZone(networkRequest, environmentDto);

        assertEquals(result, subnetAvailabilityZone);
    }
}