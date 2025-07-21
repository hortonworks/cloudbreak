package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RootDiskValidationServiceTest {

    private static final String AWS_CLOUD_PLATFORM = "AWS";

    private static final String TEST_GROUP = "test";

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private RootDiskValidationService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private DiskUpdateRequest diskUpdateRequest;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceMetadataView pgwInstanceMetadata;

    @Test
    void testValidateRootDiskResourcesForGroupAndUpdateStackTemplateBadRequestException() {
        when(defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(anyString(), eq(true))).thenReturn(100);
        when(stackDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(stackDto.getId()).thenReturn(1L);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(eq(1L))).thenReturn(Optional.of(pgwInstanceMetadata));
        when(pgwInstanceMetadata.getInstanceGroupName()).thenReturn("test");
        Template template = mock(Template.class);
        when(template.getRootVolumeType()).thenReturn("gp2");
        when(template.getRootVolumeSize()).thenReturn(400);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(stackDto.getInstanceGroupByInstanceGroupName(anyString())).thenReturn(instanceGroupDto);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateRootDiskResourcesForGroup(stackDto, TEST_GROUP, "gp2", 400));
        assertEquals("No update required.", exception.getMessage());
    }

    @Test
    void testValidateRootDiskResourcesForGroupAndUpdateStackTemplate() throws Exception {
        when(defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(anyString(), eq(true))).thenReturn(100);
        when(stackDto.getCloudPlatform()).thenReturn("AWS");
        when(stackDto.getCloudPlatform()).thenReturn(AWS_CLOUD_PLATFORM);
        when(stackDto.getId()).thenReturn(1L);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(eq(1L))).thenReturn(Optional.of(pgwInstanceMetadata));
        when(pgwInstanceMetadata.getInstanceGroupName()).thenReturn("test");
        when(instanceMetaDataService.anyInvalidMetadataForVerticalScaleInGroup(anyLong(), anyString())).thenReturn(false);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        Template template = new Template();
        template.setRootVolumeType("gp3");
        template.setRootVolumeSize(400);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getTemplate()).thenReturn(template);
        when(stackDto.getInstanceGroupByInstanceGroupName(TEST_GROUP)).thenReturn(instanceGroupDto);

        underTest.validateRootDiskResourcesForGroup(stackDto, TEST_GROUP, "gp2", 400);
    }

    @Test
    void testValidateRootDiskResourcesForGroupAndUpdateStackTemplateBadRequestExceptionUnsupportedPlatform() {
        when(stackDto.getCloudPlatform()).thenReturn("GCP");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateRootDiskResourcesForGroup(stackDto, TEST_GROUP, "gp2", 400));
        assertEquals("Root Volume Update is not supported for cloud platform: GCP and volume type: gp2", exception.getMessage());
    }
}
