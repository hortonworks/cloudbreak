package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsAdditionalDiskAttachmentService;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsAdditionalDiskCreator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

@ExtendWith(MockitoExtension.class)
class AwsResourceVolumeConnectorTest {

    @Mock
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Mock
    private AwsAdditionalDiskCreator awsAdditionalDiskCreator;

    @Mock
    private AwsAdditionalDiskAttachmentService awsAdditionalDiskAttachmentService;

    @InjectMocks
    private AwsResourceVolumeConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudResource cloudResource;

    @Test
    void testDetachVolumes() throws Exception {
        underTest.detachVolumes(authenticatedContext, List.of(cloudResource));
        verify(awsCommonDiskUpdateService).detachVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    void testDetachVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).detachVolumes(any(), anyList());
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.detachVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void testDeleteVolumes() throws Exception {
        underTest.deleteVolumes(authenticatedContext, List.of(cloudResource));
        verify(awsCommonDiskUpdateService).deleteVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    void testDeleteVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).deleteVolumes(authenticatedContext,
                List.of(cloudResource));
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.deleteVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void testUpdateDiskVolumes() throws Exception {
        underTest.updateDiskVolumes(authenticatedContext, List.of("TEST-VOLUME"), "TEST", 100);
        verify(awsCommonDiskUpdateService).modifyVolumes(authenticatedContext, List.of("TEST-VOLUME"), "TEST", 100);
    }

    @Test
    void testUpdateDiskVolumesException() throws Exception {
        doThrow(AwsServiceException.builder().message("TEST").build()).when(awsCommonDiskUpdateService).modifyVolumes(authenticatedContext,
                List.of("TEST-VOLUME"), "TEST", 100);
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.updateDiskVolumes(authenticatedContext,
                List.of("TEST-VOLUME"), "TEST", 100));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    void testCreateVolumes() {
        Group group = mock(Group.class);
        VolumeSetAttributes.Volume volumeRequest = mock(VolumeSetAttributes.Volume.class);
        CloudResource cloudResource = mock(CloudResource.class);
        CloudStack cloudStack = mock(CloudStack.class);
        underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(cloudResource));
        verify(awsAdditionalDiskCreator).createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(cloudResource));
    }

    @Test
    void testCreateVolumesThrowsException() {
        Group group = mock(Group.class);
        VolumeSetAttributes.Volume volumeRequest = mock(VolumeSetAttributes.Volume.class);
        CloudResource cloudResource = mock(CloudResource.class);
        CloudStack cloudStack = mock(CloudStack.class);
        doThrow(new CloudbreakServiceException("TEST")).when(awsAdditionalDiskCreator).createVolumes(authenticatedContext, group,
                volumeRequest, cloudStack, 2, List.of(cloudResource));
        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () -> underTest.createVolumes(authenticatedContext, group,
                volumeRequest, cloudStack, 2, List.of(cloudResource)));
        verify(awsAdditionalDiskCreator).createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(cloudResource));
        assertEquals("TEST", ex.getMessage());
    }

    @Test
    void testAttachVolumes() {
        CloudResource cloudResource = mock(CloudResource.class);
        underTest.attachVolumes(authenticatedContext, List.of(cloudResource), null);
        verify(awsAdditionalDiskAttachmentService).attachAllVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    void testGetRootVolumes() throws Exception {
        Group group = mock(Group.class);
        CloudResource cloudResource = mock(CloudResource.class);
        List<CloudResource> cloudResourceList = List.of(cloudResource);
        doReturn(cloudResourceList).when(awsCommonDiskUpdateService).getRootVolumes(authenticatedContext, group);
        RootVolumeFetchDto rootVolumeFetchDto = new RootVolumeFetchDto(authenticatedContext, group, "", List.of(mock(CloudResource.class)));

        List<CloudResource> result = underTest.getRootVolumes(rootVolumeFetchDto);

        assertEquals(cloudResourceList, result);
        verify(awsCommonDiskUpdateService).getRootVolumes(authenticatedContext, group);
    }
}