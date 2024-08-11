package com.sequenceiq.cloudbreak.cloud.gcp.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.InstanceGroups;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GcpInstanceGroupResourceBuilderTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

    private static final String PROJECT_ID = "projectId";

    private static final String INSTANCE_GROUP_NAME = "instanceGroup";

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpInstanceGroupResourceBuilder underTest;

    @Mock
    private Network network;

    @Mock
    private GcpContext gcpContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Compute compute;

    @Mock
    private InstanceGroups instanceGroups;

    @Mock
    private InstanceGroups.Delete instanceGroupsDelete;

    @Mock
    private Operation operation;

    @Mock
    private Security security;

    @Mock
    private Group group;

    @BeforeEach
    void setup() {
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
    }

    @Test
    void testDeleteWhenEverythingGoesFine() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName(INSTANCE_GROUP_NAME)
                .withInstanceId("id-123")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.delete(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), eq(INSTANCE_GROUP_NAME))).thenReturn(instanceGroupsDelete);
        when(instanceGroupsDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        assertEquals(ResourceType.GCP_INSTANCE_GROUP, delete.getType());
        assertEquals(CommonStatus.CREATED, delete.getStatus());
        assertEquals(INSTANCE_GROUP_NAME, delete.getName());
        assertEquals("master", delete.getGroup());
        assertEquals("id-123", delete.getInstanceId());
    }

    @Test
    void testCreateWhenEverythingGoesFine() throws Exception {

        when(gcpContext.getName()).thenReturn("name");
        when(group.getName()).thenReturn("group");
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, group, network);

        assertEquals("name-group-111", cloudResource.getName());
        assertNull(cloudResource.getAvailabilityZone());
    }

    @Test
    void testCreateWithAzWhenEverythingGoesFine() throws Exception {

        when(gcpContext.getName()).thenReturn("name");
        when(group.getName()).thenReturn("group");
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, group, network, AVAILABILITY_ZONE);

        assertEquals("name-group-111-c", cloudResource.getName());
        assertEquals(AVAILABILITY_ZONE, cloudResource.getAvailabilityZone());
    }

    @Test
    void testCloudResourceBoundToStack() throws Exception {
        when(gcpContext.getName()).thenReturn("name");
        when(group.getName()).thenReturn("group");
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        CloudResource cloudResource1 = underTest.create(gcpContext, authenticatedContext, group, network);

        reset(authenticatedContext);
        when(authenticatedContext.getCloudContext().getId()).thenReturn(222L);
        CloudResource cloudResource2 = underTest.create(gcpContext, authenticatedContext, group, network);

        assertNotEquals(cloudResource1.getName(), cloudResource2.getName());
    }

    @Test
    void testBuildWithItemsInGroup() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withInstanceId("id-123")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        Compute.InstanceGroups.Insert instanceGroupsInsert = mock(Compute.InstanceGroups.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.insert(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), any())).thenReturn(instanceGroupsInsert);
        when(instanceGroupsInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        CloudResource cloudResource = underTest.build(gcpContext, authenticatedContext, group, network, security, resource);

        assertEquals("super", cloudResource.getName());

    }

    @Test
    void testBuildNoPermission() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withInstanceId("id-123")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        Compute.InstanceGroups.Insert instanceGroupsInsert = mock(Compute.InstanceGroups.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.insert(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), any())).thenReturn(instanceGroupsInsert);
        when(instanceGroupsInsert.execute()).thenReturn(operation);
        when(operation.getHttpErrorStatusCode()).thenReturn(401);
        when(operation.getHttpErrorMessage()).thenReturn("Not Authorized");

        assertThrows(GcpResourceException.class,
                () -> underTest.build(gcpContext, authenticatedContext, group, network, security, resource), "Not Authorized");
    }

    @Test
    void testResourceType() {
        assertTrue(underTest.resourceType().equals(ResourceType.GCP_INSTANCE_GROUP));
    }

}
