package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.CloudConnectResources;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class RootDiskValidationServiceTest {

    private static final String AWS_CLOUD_PLATFORM = "AWS";

    private static final String AZURE_CLOUD_PLATFORM = "AZURE";

    private static final String TEST_GROUP = "test";

    private static final String TEST_INSTANCE_ID = "test-instance-id";

    private static final Long TEST_STACK_ID = 1L;

    @Mock
    private ResourceService resourceService;

    @Mock
    private CloudConnectorHelper cloudConnectorHelper;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private RootDiskValidationService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private DiskUpdateRequest diskUpdateRequest;

    @Mock
    private CloudConnectResources cloudConnectResources;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudConnector connector;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Group group;

    @Mock
    private Resource resource;

    @BeforeEach
    void setUp() {
        when(stackDto.getId()).thenReturn(TEST_STACK_ID);
        when(diskUpdateRequest.getGroup()).thenReturn(TEST_GROUP);
        when(diskUpdateRequest.getSize()).thenReturn(400);
        when(cloudConnectResources.getCloudStack()).thenReturn(cloudStack);
        when(cloudConnectResources.getCloudConnector()).thenReturn(connector);
        when(cloudConnectResources.getAuthenticatedContext()).thenReturn(ac);
        when(cloudConnectorHelper.getCloudConnectorResources(stackDto)).thenReturn(cloudConnectResources);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(group.getName()).thenReturn(TEST_GROUP);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getInstanceId()).thenReturn(TEST_INSTANCE_ID);
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
        when(resource.getInstanceId()).thenReturn(TEST_INSTANCE_ID);
    }

    @Test
    void testFetchRootDiskResourcesForGroupWhenResourceAlreadyStored() throws Exception {
        when(stackDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(diskUpdateRequest.getDiskType()).thenReturn(DiskType.ROOT_DISK);
        List<Resource> resourceList = List.of(resource);
        when(resourceService.findByStackIdAndType(TEST_STACK_ID, ResourceType.AWS_ROOT_DISK)).thenReturn(resourceList);
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("", "/dev/xvda", 200, "gp2", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "", List.of(volume), 400, "gp2");
        when(resource.getAttributes()).thenReturn(new Json(volumeSetAttributes));
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        Template template = new Template();
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(stackDto.getInstanceGroupByInstanceGroupName(TEST_GROUP)).thenReturn(instanceGroupDto);

        List<Resource> result = underTest.fetchRootDiskResourcesForGroup(stackDto, diskUpdateRequest);
        verify(resourceService).deleteAll(resourceList);
        verify(templateService).savePure(template);
        assertEquals(400, template.getRootVolumeSize());
        assertEquals(resourceList, result);

    }

    @Test
    void testFetchRootDiskResourcesForGroupWhenResourceAlreadyStoredBadRequestException() {
        when(stackDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(diskUpdateRequest.getVolumeType()).thenReturn("gp2");
        when(resourceService.findByStackIdAndType(TEST_STACK_ID, ResourceType.AWS_ROOT_DISK)).thenReturn(List.of(resource));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("", "/dev/xvda", 400, "gp2", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "", List.of(volume), 400, "gp2");
        when(resource.getAttributes()).thenReturn(new Json(volumeSetAttributes));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.fetchRootDiskResourcesForGroup(stackDto, diskUpdateRequest));
        assertEquals("No update required.", exception.getMessage());
    }

    @Test
    void testFetchRootDiskResourcesForGroupWhenResourceAlreadyStoredGenericExceptionWhileParsingAttributes() throws Exception {
        when(stackDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(diskUpdateRequest.getDiskType()).thenReturn(DiskType.ROOT_DISK);
        List<Resource> resourceList = List.of(resource);
        when(resourceService.findByStackIdAndType(TEST_STACK_ID, ResourceType.AWS_ROOT_DISK)).thenReturn(resourceList);
        when(resource.getAttributes()).thenThrow(new RuntimeException("Test"));
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        Template template = new Template();
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(stackDto.getInstanceGroupByInstanceGroupName(TEST_GROUP)).thenReturn(instanceGroupDto);

        List<Resource> result = underTest.fetchRootDiskResourcesForGroup(stackDto, diskUpdateRequest);
        verify(resourceService).deleteAll(resourceList);
        verify(templateService).savePure(template);
        assertEquals(400, template.getRootVolumeSize());
        assertEquals(resourceList, result);
    }

    @Test
    void testFetchRootDiskResourcesForGroupWhenRootDiskResourceNotInResourceTable() throws Exception {
        when(stackDto.getCloudPlatform()).thenReturn(AZURE_CLOUD_PLATFORM);
        when(diskUpdateRequest.getDiskType()).thenReturn(DiskType.ROOT_DISK);
        ResourceVolumeConnector resourceVolumeConnector = mock(ResourceVolumeConnector.class);
        when(connector.volumeConnector()).thenReturn(resourceVolumeConnector);
        List<Resource> resourceList = List.of(resource);
        CloudResource cloudResource = mock(CloudResource.class);
        when(resourceVolumeConnector.getRootVolumes(any(RootVolumeFetchDto.class))).thenReturn(List.of(cloudResource));
        when(resourceToCloudResourceConverter.convert(resource)).thenReturn(cloudResource);
        when(cloudResourceToResourceConverter.convert(cloudResource)).thenReturn(resource);
        when(resourceService.findByStackIdAndType(TEST_STACK_ID, ResourceType.AZURE_DISK)).thenReturn(List.of());
        when(resourceService.findByStackIdAndType(TEST_STACK_ID, ResourceType.AZURE_INSTANCE)).thenReturn(resourceList);
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("", "/dev/xvda", 200, "gp2", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("az", true, "", List.of(volume), 400, "gp2");
        when(resource.getAttributes()).thenReturn(new Json(volumeSetAttributes));
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        Template template = new Template();
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(stackDto.getInstanceGroupByInstanceGroupName(TEST_GROUP)).thenReturn(instanceGroupDto);

        List<Resource> result = underTest.fetchRootDiskResourcesForGroup(stackDto, diskUpdateRequest);
        verify(resourceService).deleteAll(resourceList);
        verify(templateService).savePure(template);
        assertEquals(400, template.getRootVolumeSize());
        assertEquals(resourceList, result);
        verify(resourceVolumeConnector).getRootVolumes(any(RootVolumeFetchDto.class));
    }
}
