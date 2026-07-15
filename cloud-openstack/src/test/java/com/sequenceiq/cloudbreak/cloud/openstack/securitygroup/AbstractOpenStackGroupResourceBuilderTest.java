package com.sequenceiq.cloudbreak.cloud.openstack.securitygroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AbstractOpenStackGroupResourceBuilderTest {

    private static final String EXISTING_SG_ID = "pre-existing-sg-id";

    private static final String GROUP_NAME = "worker";

    private static final Long STACK_ID = 1L;

    @Mock
    private OpenStackResourceNameService resourceNameService;

    @InjectMocks
    private OpenStackSecurityGroupResourceBuilder underTest;

    @Test
    void createShouldReturnExistingSecurityGroupResourceWhenCloudSecurityIdPresent() {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        Group group = mock(Group.class);
        Network network = mock(Network.class);
        Security security = new Security(Collections.emptyList(), List.of(EXISTING_SG_ID));

        when(group.getSecurity()).thenReturn(security);
        when(group.getName()).thenReturn(GROUP_NAME);

        CloudResource result = underTest.create(context, auth, group, network);

        assertNotNull(result);
        assertEquals(EXISTING_SG_ID, result.getName());
        assertEquals(EXISTING_SG_ID, result.getReference());
        assertEquals(CommonStatus.CREATED, result.getStatus());
        assertEquals(ResourceType.OPENSTACK_SECURITY_GROUP, result.getType());
        assertEquals(GROUP_NAME, result.getGroup());
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = result.getParameter(CloudResource.ATTRIBUTES, Map.class);
        assertEquals(Boolean.TRUE, attributes.get(AbstractOpenStackGroupResourceBuilder.EXISTING_SECURITY_GROUP));
        verifyNoInteractions(resourceNameService);
    }

    @Test
    void createShouldGenerateNewResourceWhenNoExistingSecurityGroup() {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Group group = mock(Group.class);
        Network network = mock(Network.class);
        Security security = new Security(Collections.emptyList(), Collections.emptyList());

        when(group.getSecurity()).thenReturn(security);
        when(group.getName()).thenReturn(GROUP_NAME);
        when(context.getName()).thenReturn("my-stack");
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(resourceNameService.resourceName(any(), any(), any(), any())).thenReturn("generated-sg-name");

        CloudResource result = underTest.create(context, auth, group, network);

        assertNotNull(result);
        assertEquals("generated-sg-name", result.getName());
        assertNull(result.getReference());
        assertEquals(CommonStatus.REQUESTED, result.getStatus());
        verify(resourceNameService).resourceName(ResourceType.OPENSTACK_SECURITY_GROUP, "my-stack", GROUP_NAME, STACK_ID);
    }

    @Test
    void createShouldGenerateNewResourceWhenSecurityIsNull() {
        OpenStackContext context = mock(OpenStackContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Group group = mock(Group.class);
        Network network = mock(Network.class);

        when(group.getSecurity()).thenReturn(null);
        when(group.getName()).thenReturn(GROUP_NAME);
        when(context.getName()).thenReturn("my-stack");
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(resourceNameService.resourceName(any(), any(), any(), any())).thenReturn("generated-sg-name");

        CloudResource result = underTest.create(context, auth, group, network);

        assertNotNull(result);
        assertEquals("generated-sg-name", result.getName());
        assertNull(result.getReference());
        assertEquals(CommonStatus.REQUESTED, result.getStatus());
    }
}
