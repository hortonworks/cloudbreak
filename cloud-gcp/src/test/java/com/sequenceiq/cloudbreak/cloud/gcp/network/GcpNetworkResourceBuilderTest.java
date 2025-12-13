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
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpNetworkResourceBuilderTest {

    @InjectMocks
    private GcpNetworkResourceBuilder underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpResourceNameService resourceNameService;

    @Test
    public void testCreateWhenExistingShouldReturnWithCustomNetworkId() {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.isExistingNetwork(network)).thenReturn(true);
        when(gcpStackUtil.getCustomNetworkId(network)).thenReturn("custom");

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, network);

        assertEquals("custom", cloudResource.getName());
    }

    @Test
    public void testCreateWhenNOTExistingShouldReturnWithNewResourceName() {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.isExistingNetwork(network)).thenReturn(false);
        when(resourceNameService.network(any())).thenReturn("test");

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
        Compute.Networks networks = mock(Compute.Networks.class);
        Compute.Networks.Delete networksDelete = mock(Compute.Networks.Delete.class);
        Operation operation = mock(Operation.class);

        when(gcpStackUtil.isExistingNetwork(network)).thenReturn(false);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(compute.networks()).thenReturn(networks);
        when(networks.delete(anyString(), anyString())).thenReturn(networksDelete);
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

        when(gcpStackUtil.isExistingNetwork(network)).thenReturn(true);

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        assertEquals(null, delete);
    }

    @Test
    public void testResourceType() {
        assertTrue(underTest.resourceType().equals(ResourceType.GCP_NETWORK));
    }

    @Test
    public void testOrder() {
        assertTrue(underTest.order() == 0);
    }
}
