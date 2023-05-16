package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;

@ExtendWith(MockitoExtension.class)
public class AwsCommonDiskUpdateServiceTest {

    @Spy
    private AwsCommonDiskUpdateService underTest;

    @Test
    void testModifyVolumes() throws Exception {
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        VolumeModification res = VolumeModification.builder().modificationState(VolumeModificationState.COMPLETED).build();
        DescribeVolumesModificationsResponse volumesModificationsResponse = DescribeVolumesModificationsResponse.builder().volumesModifications(List.of(res))
                .build();
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(List.of("vol-1")).build();
        when(amazonEc2Client.describeVolumeModification(describeVolumesModificationsRequest)).thenReturn(volumesModificationsResponse);
        when(underTest.getEc2Client(authenticatedContext)).thenReturn(amazonEc2Client);
        underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100);
        verify(underTest, times(1)).getVolumeModificationsState(List.of("vol-1"), amazonEc2Client);
    }

    @Test
    void testModifyVolumesException() throws Exception {
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(amazonEc2Client.modifyVolume(any(ModifyVolumeRequest.class))).thenThrow(Ec2Exception.builder().message("TEST").build());
        when(underTest.getEc2Client(authenticatedContext)).thenReturn(amazonEc2Client);
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100));
        assertEquals("Exception while modifying disk volumes: TEST", exception.getMessage());
    }

    @Test
    void testModifyVolumesModificationLimitReached() throws Exception {
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        ArgumentCaptor<ModifyVolumeRequest> modifyVolumeRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyVolumeRequest.class);
        when(amazonEc2Client.modifyVolume(any(ModifyVolumeRequest.class))).thenThrow(Ec2Exception.builder()
                .message("You've reached the maximum modification rate per volume limit.").build());
        when(underTest.getEc2Client(authenticatedContext)).thenReturn(amazonEc2Client);
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100));
        assertEquals("Exception while modifying disk volumes: You've reached the maximum modification rate per volume limit.",
                exception.getMessage());
        verify(amazonEc2Client, times(1)).modifyVolume(modifyVolumeRequestArgumentCaptor.capture());
        assertEquals("vol-1", modifyVolumeRequestArgumentCaptor.getAllValues().get(0).volumeId());
        assertEquals("gp2", modifyVolumeRequestArgumentCaptor.getAllValues().get(0).volumeType().toString());
        assertEquals(100, modifyVolumeRequestArgumentCaptor.getAllValues().get(0).size());
    }

    @Test
    void testModifyVolumesExceptionThrown() throws Exception {
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        when(amazonEc2Client.modifyVolume(any(ModifyVolumeRequest.class))).thenThrow(Ec2Exception.builder().message("TEST").build());
        when(underTest.getEc2Client(authenticatedContext)).thenReturn(amazonEc2Client);
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100));
        assertEquals("Exception while modifying disk volumes: TEST", exception.getMessage());
    }

}
