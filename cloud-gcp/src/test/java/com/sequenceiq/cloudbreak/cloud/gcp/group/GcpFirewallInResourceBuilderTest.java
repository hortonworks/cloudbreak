package com.sequenceiq.cloudbreak.cloud.gcp.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpFirewallInResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpFirewallInResourceBuilderTest {

    @Mock
    private GcpResourceNameService resourceNameService;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpFirewallInResourceBuilder underTest;

    @Test
    public void testDeleteWhenEverythingGoesFine() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_FIREWALL_IN)
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
        Compute.Firewalls firewalls = mock(Compute.Firewalls.class);
        Compute.Firewalls.Delete firewallsDelete = mock(Compute.Firewalls.Delete.class);
        Operation operation = mock(Operation.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(compute.firewalls()).thenReturn(firewalls);
        when(firewalls.delete(anyString(), anyString())).thenReturn(firewallsDelete);
        when(firewallsDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        assertEquals(ResourceType.GCP_FIREWALL_IN, delete.getType());
        assertEquals(CommonStatus.CREATED, delete.getStatus());
        assertEquals("super", delete.getName());
        assertEquals("master", delete.getGroup());
        assertEquals("id-123", delete.getInstanceId());
    }

    @Test
    public void testCreateWhenEverythingGoesFine() throws Exception {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);
        Group group = mock(Group.class);

        when(gcpContext.getName()).thenReturn("name");
        when(gcpStackUtil.noFirewallRules(network)).thenReturn(false);
        when(resourceNameService.firewallIn(any())).thenReturn("test");

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, group, network);

        assertEquals("test", cloudResource.getName());
    }

    @Test
    public void testCreateWhenFirewallShouldNotBeCreated() throws Exception {
        GcpContext gcpContext = mock(GcpContext.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);
        Group group = mock(Group.class);

        when(gcpStackUtil.noFirewallRules(network)).thenReturn(true);

        ResourceNotNeededException resourceNotNeededException = assertThrows(ResourceNotNeededException.class,
                () -> underTest.create(gcpContext, authenticatedContext, group, network));

        assertEquals("Firewall rules won't be created.", resourceNotNeededException.getMessage());
    }

    @Test
    public void testResourceType() {
        assertTrue(underTest.resourceType().equals(ResourceType.GCP_FIREWALL_IN));
    }

    @Test
    public void testOrder() {
        assertTrue(underTest.order() == 0);
    }
}
