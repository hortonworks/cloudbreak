package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.template.OrderedBuilder.LOWEST_PRECEDENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.Instance;

@ExtendWith({ MockitoExtension.class })
class AwsRootVolumeResourceBuilderTest {

    @InjectMocks
    private AwsRootVolumeResourceBuilder underTest;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private VolumeBuilderUtil volumeBuilderUtil;

    @Mock
    private AwsResourceNameService resourceNameService;

    @Mock
    private AwsContext context;

    @Mock
    private CloudInstance instance;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private Group group;

    @Mock
    private Image image;

    @Test
    void testCreate() {
        when(context.getName()).thenReturn("test");
        when(group.getName()).thenReturn("test-group");
        CloudContext cloudContext = mock(CloudContext.class);
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(1L);
        when(instance.getAvailabilityZone()).thenReturn("az");
        when(resourceNameService.rootDisk(eq("test"), eq(1L), eq("test-group"), eq(1L))).thenReturn("test-1-test-group-1");
        CloudResource cloudResource = CloudResource.builder().withName("test-1-test-group-1").withType(ResourceType.AWS_ROOT_DISK).withGroup("test-group")
                .withAvailabilityZone("az").withPersistent(true).withParameters(new HashMap<>()).withStatus(CommonStatus.REQUESTED).build();
        when(volumeBuilderUtil.createRootVolumeResource(eq("test-1-test-group-1"), eq("test-group"), eq(ResourceType.AWS_ROOT_DISK), eq("az")))
                .thenReturn(cloudResource);

        List<CloudResource> result = underTest.create(context, instance, 1L, auth, group, image);
        CloudResource responseResource = result.getFirst();
        assertEquals("test-1-test-group-1", responseResource.getName());
        assertEquals("test-group", responseResource.getGroup());
        assertEquals(ResourceType.AWS_ROOT_DISK, responseResource.getType());
        assertEquals(CommonStatus.REQUESTED, responseResource.getStatus());
        assertEquals("az", responseResource.getAvailabilityZone());
        assertTrue(responseResource.isPersistent());
        assertEquals(0, responseResource.getParameters().size());
    }

    @Test
    void testBuild() throws Exception {
        CloudResource cloudResource = CloudResource.builder().withName("test").withType(ResourceType.AWS_ROOT_DISK)
                .withParameters(new HashMap<>()).withStatus(CommonStatus.REQUESTED).withInstanceId("test-instance").build();
        CloudResource responseResource = CloudResource.builder().withName("test-response").withType(ResourceType.AWS_ROOT_DISK)
                .withParameters(new HashMap<>()).withStatus(CommonStatus.CREATED).withInstanceId("test-instance").build();
        List<CloudResource> expectedUpdateResources = List.of(responseResource);
        when(context.getComputeResources(1L)).thenReturn(List.of(cloudResource));
        Instance cloudInstance = mock(Instance.class);
        List<Instance> instances = List.of(cloudInstance);
        when(volumeBuilderUtil.describeInstancesByInstanceIds(any(), eq(auth))).thenReturn(instances);
        List<String> rootVolIds = List.of("1");
        when(volumeBuilderUtil.getRootVolumeIdsFromInstances(eq(instances))).thenReturn(rootVolIds);
        when(volumeBuilderUtil.updateRootVolumeResource(any(), eq(rootVolIds), eq(auth))).thenReturn(expectedUpdateResources);
        CloudContext cloudContext = mock(CloudContext.class);
        when(auth.getCloudContext()).thenReturn(cloudContext);

        List<CloudResource> result = underTest.build(context, instance, 1L, auth, group, List.of(cloudResource), mock(CloudStack.class));

        verify(resourceNotifier).notifyUpdates(eq(expectedUpdateResources), eq(cloudContext));
        assertEquals(0, result.size());
    }

    @Test
    void testDelete() {
        CloudResource resource = mock(CloudResource.class);
        CloudResource result = underTest.delete(context, auth, resource);
        assertEquals(resource, result);
    }

    @Test
    void testResourceType() {
        assertEquals(ResourceType.AWS_ROOT_DISK, underTest.resourceType());
    }

    @Test
    void testOrder() {
        assertEquals(LOWEST_PRECEDENCE, underTest.order());
    }
}
