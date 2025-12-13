package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.RegionList;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.common.model.PrivateEndpointType;

@ExtendWith(MockitoExtension.class)
public class GcpCloudSubnetProviderTest {

    @InjectMocks
    public GcpCloudSubnetProvider underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Test
    public void testProvideCloudSubnetsWhen2Subnet2AZIsProvidedShouldCreate2NewSubnetWithDifferentAzShouldBeProvided() throws IOException {
        NetworkCreationRequest request = new NetworkCreationRequest.Builder()
                .withEnvName("envName")
                .withEnvId(1L)
                .withNetworkCidr("10.0.0.0/16")
                .withRegion(Region.region("euwest1"))
                .withPrivateSubnetEnabled(true)
                .withCloudCredential(mock(CloudCredential.class))
                .withEndpointType(PrivateEndpointType.USE_VPC_ENDPOINT)
                .build();

        Compute compute = mock(Compute.class);
        Compute.Regions regions = mock(Compute.Regions.class);
        Compute.Regions.List regionsList = mock(Compute.Regions.List.class);
        RegionList regionListObject = mock(RegionList.class);

        com.google.api.services.compute.model.Region regionObject = new com.google.api.services.compute.model.Region();
        regionObject.setName("euwest1");
        regionObject.setZones(List.of("euwest1/euwest1a", "euwest1/euwest1b"));

        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(compute.regions()).thenReturn(regions);
        when(regions.list(anyString())).thenReturn(regionsList);
        when(regionsList.execute()).thenReturn(regionListObject);
        when(regionListObject.getItems()).thenReturn(List.of(regionObject));

        List<CreatedSubnet> provide = underTest.provide(request, List.of("10.0.0.0/16", "10.0.0.1/16"));
        assertEquals(2, provide.size());
        assertTrue(provide.stream()
                .map(e -> e.getAvailabilityZone())
                .filter(e -> e.equals("euwest1a"))
                .findFirst()
                .isPresent());
        assertTrue(provide.stream()
                .map(e -> e.getAvailabilityZone())
                .filter(e -> e.equals("euwest1b"))
                .findFirst()
                .isPresent());
    }

    @Test
    public void testProvideCloudSubnetsWhen2Subnet1AZIsProvidedShouldCreate2NewSubnetWith1AzShouldBeProvided() throws IOException {
        NetworkCreationRequest request = new NetworkCreationRequest.Builder()
                .withEnvName("envName")
                .withEnvId(1L)
                .withNetworkCidr("10.0.0.0/16")
                .withRegion(Region.region("euwest1"))
                .withPrivateSubnetEnabled(true)
                .withCloudCredential(mock(CloudCredential.class))
                .withEndpointType(PrivateEndpointType.USE_VPC_ENDPOINT)
                .build();

        Compute compute = mock(Compute.class);
        Compute.Regions regions = mock(Compute.Regions.class);
        Compute.Regions.List regionsList = mock(Compute.Regions.List.class);
        RegionList regionListObject = mock(RegionList.class);

        com.google.api.services.compute.model.Region regionObject = new com.google.api.services.compute.model.Region();
        regionObject.setName("euwest1");
        regionObject.setZones(List.of("euwest1/euwest1a"));

        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("project-id");
        when(compute.regions()).thenReturn(regions);
        when(regions.list(anyString())).thenReturn(regionsList);
        when(regionsList.execute()).thenReturn(regionListObject);
        when(regionListObject.getItems()).thenReturn(List.of(regionObject));

        List<CreatedSubnet> provide = underTest.provide(request, List.of("10.0.0.0/16", "10.0.0.1/16"));
        assertEquals(2, provide.size());
        assertTrue(provide.stream()
                .map(e -> e.getAvailabilityZone())
                .filter(e -> e.equals("euwest1a"))
                .findFirst()
                .isPresent());
        assertTrue(provide.stream()
                .map(e -> e.getAvailabilityZone())
                .filter(e -> e.equals("euwest1a"))
                .findFirst()
                .isPresent());
    }
}