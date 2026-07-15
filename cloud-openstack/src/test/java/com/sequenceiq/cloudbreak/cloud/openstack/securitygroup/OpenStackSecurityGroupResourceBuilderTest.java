package com.sequenceiq.cloudbreak.cloud.openstack.securitygroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.SecGroupExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class OpenStackSecurityGroupResourceBuilderTest {

    private static final String EXISTING_SG_ID = "existing-sg-12345";

    private static final String GROUP_NAME = "master";

    @Mock
    private OpenStackResourceNameService resourceNameService;

    @Mock
    private OpenStackClient openStackClient;

    @InjectMocks
    private OpenStackSecurityGroupResourceBuilder underTest;

    @Test
    void buildShouldSkipCreationWhenExistingSecurityGroupProvided() throws Exception {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        Group group = mock(Group.class);
        Network network = mock(Network.class);
        Security security = new Security(Collections.emptyList(), List.of(EXISTING_SG_ID));

        when(group.getName()).thenReturn(GROUP_NAME);

        CloudResource existingResource = CloudResource.builder()
                .withName(EXISTING_SG_ID)
                .withGroup(GROUP_NAME)
                .withType(ResourceType.OPENSTACK_SECURITY_GROUP)
                .withReference(EXISTING_SG_ID)
                .withStatus(CommonStatus.CREATED)
                .build();

        CloudResource result = underTest.build(context, auth, group, network, security, existingResource);

        assertEquals(EXISTING_SG_ID, result.getReference());
        assertEquals(CommonStatus.CREATED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = result.getParameter(CloudResource.ATTRIBUTES, Map.class);
        assertEquals(Boolean.TRUE, attributes.get(AbstractOpenStackGroupResourceBuilder.EXISTING_SECURITY_GROUP));
    }

    @Test
    void buildShouldCreateNewSecurityGroupWhenNoExistingProvided() throws Exception {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        Group group = mock(Group.class);
        Network network = mock(Network.class);
        Security security = new Security(Collections.emptyList(), Collections.emptyList());
        Subnet subnet = new Subnet("10.0.0.0/24");

        when(group.getName()).thenReturn(GROUP_NAME);
        when(network.getSubnet()).thenReturn(subnet);

        OSClient<?> osClient = mock(OSClient.class);
        ComputeService computeService = mock(ComputeService.class);
        ComputeSecurityGroupService sgService = mock(ComputeSecurityGroupService.class);
        SecGroupExtension createdSg = mock(SecGroupExtension.class);

        doReturn(osClient).when(openStackClient).createOSClient(any(AuthenticatedContext.class));
        when(osClient.compute()).thenReturn(computeService);
        when(computeService.securityGroups()).thenReturn(sgService);
        when(sgService.create(anyString(), anyString())).thenReturn(createdSg);
        when(createdSg.getId()).thenReturn("new-sg-id");
        when(sgService.get("new-sg-id")).thenReturn(createdSg);
        when(createdSg.getRules()).thenReturn(Collections.emptyList());

        CloudResource namedResource = CloudResource.builder()
                .withName("cb-master-sg")
                .withGroup(GROUP_NAME)
                .withType(ResourceType.OPENSTACK_SECURITY_GROUP)
                .withStatus(CommonStatus.REQUESTED)
                .build();

        CloudResource result = underTest.build(context, auth, group, network, security, namedResource);

        assertEquals("new-sg-id", result.getReference());
        assertEquals(CommonStatus.CREATED, result.getStatus());
        verify(sgService).create(anyString(), anyString());
    }

    @Test
    void deleteShouldSkipDeletionWhenExistingSecurityGroup() throws Exception {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        CloudResource existingResource = CloudResource.builder()
                .withName(EXISTING_SG_ID)
                .withGroup(GROUP_NAME)
                .withType(ResourceType.OPENSTACK_SECURITY_GROUP)
                .withReference(EXISTING_SG_ID)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Map.of(AbstractOpenStackGroupResourceBuilder.EXISTING_SECURITY_GROUP, true))
                .build();

        CloudResource result = underTest.delete(context, auth, existingResource, network);

        assertNull(result);
    }

    @Test
    void deleteShouldDeleteCloudbreakCreatedSecurityGroup() throws Exception {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        Network network = mock(Network.class);

        OSClient<?> osClient = mock(OSClient.class);
        ComputeService computeService = mock(ComputeService.class);
        ComputeSecurityGroupService sgService = mock(ComputeSecurityGroupService.class);
        ActionResponse actionResponse = ActionResponse.actionSuccess();

        doReturn(osClient).when(openStackClient).createOSClient(any(AuthenticatedContext.class));
        when(osClient.compute()).thenReturn(computeService);
        when(computeService.securityGroups()).thenReturn(sgService);
        when(sgService.delete("cb-created-sg-id")).thenReturn(actionResponse);

        CloudResource cbCreatedResource = CloudResource.builder()
                .withName("cb-master-sg-12345")
                .withGroup(GROUP_NAME)
                .withType(ResourceType.OPENSTACK_SECURITY_GROUP)
                .withReference("cb-created-sg-id")
                .withStatus(CommonStatus.CREATED)
                .build();

        CloudResource result = underTest.delete(context, auth, cbCreatedResource, network);

        verify(sgService).delete("cb-created-sg-id");
    }
}
