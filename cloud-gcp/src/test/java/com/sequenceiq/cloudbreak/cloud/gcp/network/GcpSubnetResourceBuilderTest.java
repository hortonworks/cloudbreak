package com.sequenceiq.cloudbreak.cloud.gcp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpSubnetResourceBuilderTest {

    @Mock
    private GcpResourceNameService resourceNameService;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpSubnetResourceBuilder underTest;

    @Test
    public void testCreateWhenExistingShouldReturnWithCustomNetworkId() {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.isExistingSubnet(network)).thenReturn(true);
        when(gcpStackUtil.getSubnetId(network)).thenReturn("custom");

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, network);

        assertEquals("custom", cloudResource.getName());
    }

    @Test
    public void testCreateWhenNOTExistingShouldReturnWithNewResourceName() {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.isExistingSubnet(network)).thenReturn(false);
        when(resourceNameService.subnet(any())).thenReturn("test");

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, network);

        assertEquals("test", cloudResource.getName());
    }

    @Test
    public void testDeleteWhenEverythingGoesFine() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_NETWORK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withInstanceId("id-123")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);
        Compute compute = mock(Compute.class);
        Location location = mock(Location.class);
        Compute.Subnetworks networks = mock(Compute.Subnetworks.class);
        Compute.Subnetworks.Delete networksDelete = mock(Compute.Subnetworks.Delete.class);
        Operation operation = mock(Operation.class);

        when(gcpStackUtil.isNewNetworkAndSubnet(network)).thenReturn(true);
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(Region.region("us-west-1"));
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(compute.subnetworks()).thenReturn(networks);
        when(networks.delete(anyString(), anyString(), anyString())).thenReturn(networksDelete);
        when(networksDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        assertEquals(ResourceType.GCP_NETWORK, delete.getType());
        assertEquals(CommonStatus.CREATED, delete.getStatus());
        assertEquals("super", delete.getName());
        assertEquals("master", delete.getGroup());
        assertEquals("id-123", delete.getInstanceId());
    }

    @Test
    public void testDeleteWhenExistingEverythingGoesFineShouldReturnNull() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_NETWORK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withInstanceId("id-123")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.isNewNetworkAndSubnet(network)).thenReturn(false);
        when(gcpStackUtil.isNewSubnetInExistingNetwork(network)).thenReturn(false);

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        assertEquals(null, delete);
    }

    @Test
    public void testResourceType() {
        assertTrue(underTest.resourceType().equals(ResourceType.GCP_SUBNET));
    }

    @Test
    public void testOrder() {
        assertTrue(underTest.order() == 1);
    }
}
