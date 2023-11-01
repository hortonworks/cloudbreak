package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@ExtendWith(MockitoExtension.class)
public class AwsCommonDiskUpdateServiceTest {

    @Spy
    private AwsCommonDiskUpdateService underTest;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void setUp() {
        doReturn(amazonEc2Client).when(underTest).getEc2Client(authenticatedContext);
    }

    @Test
    void testModifyVolumes() throws Exception {
        VolumeModification res = VolumeModification.builder().modificationState(VolumeModificationState.COMPLETED).build();
        DescribeVolumesModificationsResponse volumesModificationsResponse = DescribeVolumesModificationsResponse.builder().volumesModifications(List.of(res))
                .build();
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(List.of("vol-1")).build();
        when(amazonEc2Client.describeVolumeModifications(describeVolumesModificationsRequest)).thenReturn(volumesModificationsResponse);
        underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100);
        verify(underTest, times(2)).getVolumeModificationsState(List.of("vol-1"), amazonEc2Client);
    }

    @Test
    void testModifyVolumesModificationLimitReached() throws Exception {
        ArgumentCaptor<ModifyVolumeRequest> modifyVolumeRequestArgumentCaptor = ArgumentCaptor.forClass(ModifyVolumeRequest.class);
        when(amazonEc2Client.modifyVolume(any(ModifyVolumeRequest.class))).thenThrow(Ec2Exception.builder()
                .message("You've reached the maximum modification rate per volume limit.").build());
        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100));
        assertEquals("Exception while modifying disk volume: vol-1, exception: You've reached the maximum modification rate per volume limit.",
                exception.getMessage());
        verify(amazonEc2Client, times(1)).modifyVolume(modifyVolumeRequestArgumentCaptor.capture());
        assertEquals("vol-1", modifyVolumeRequestArgumentCaptor.getAllValues().get(0).volumeId());
        assertEquals("gp2", modifyVolumeRequestArgumentCaptor.getAllValues().get(0).volumeType().toString());
        assertEquals(100, modifyVolumeRequestArgumentCaptor.getAllValues().get(0).size());
    }

    @Test
    public void testDetachVolumes() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn("TEST-1").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn("vol-test-1").when(volume).getId();
        doAnswer(new Answer<DescribeVolumesResponse>() {
            private int count;
            public DescribeVolumesResponse answer(InvocationOnMock invocation) {
                if (count++ == 1) {
                    software.amazon.awssdk.services.ec2.model.Volume volumeResponse = software.amazon.awssdk.services.ec2.model.Volume.builder()
                            .state(VolumeState.IN_USE).build();
                    return DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
                }
                software.amazon.awssdk.services.ec2.model.Volume volumeResponse = software.amazon.awssdk.services.ec2.model.Volume.builder()
                        .state(VolumeState.AVAILABLE).build();
                return DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
            }
        }).when(amazonEc2Client).describeVolumes(any());

        underTest.detachVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    public void testDetachVolumesException() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn("TEST-1").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn("vol-test-1").when(volume).getId();
        software.amazon.awssdk.services.ec2.model.Volume volumeResponse = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .volumeId("vol-test-1").state(VolumeState.IN_USE).build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
        doReturn(response).when(amazonEc2Client).describeVolumes(any());
        doThrow(AwsServiceException.builder().message("TEST").build()).when(amazonEc2Client).detachVolume(any());
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.detachVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void testDeleteVolumes() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn("TEST-1").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn("vol-test-1").when(volume).getId();
        underTest.deleteVolumes(authenticatedContext, List.of(cloudResource));
    }
}
