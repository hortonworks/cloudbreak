package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
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
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@ExtendWith(MockitoExtension.class)
class AwsCommonDiskUpdateServiceTest {

    @Mock
    private CommonAwsClient commonAwsClient;

    @InjectMocks
    private AwsCommonDiskUpdateService underTest;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Test
    void testModifyVolumes() throws Exception {
        doReturn(amazonEc2Client).when(authenticatedContext).getParameter(any());
        VolumeModification res = VolumeModification.builder().modificationState(VolumeModificationState.COMPLETED).build();
        DescribeVolumesModificationsResponse volumesModificationsResponse = DescribeVolumesModificationsResponse.builder().volumesModifications(List.of(res))
                .build();
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = DescribeVolumesModificationsRequest.builder()
                .volumeIds(List.of("vol-1")).build();
        when(amazonEc2Client.describeVolumeModifications(describeVolumesModificationsRequest)).thenReturn(volumesModificationsResponse);
        underTest.modifyVolumes(authenticatedContext, List.of("vol-1"), "gp2", 100);
        verify(amazonEc2Client, times(2)).describeVolumeModifications(any());
    }

    @Test
    void testModifyVolumesModificationLimitReached() throws Exception {
        doReturn(amazonEc2Client).when(authenticatedContext).getParameter(any());
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
    void testDetachVolumes() throws Exception {
        doReturn(amazonEc2Client).when(authenticatedContext).getParameter(any());
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
                            .volumeId("vol-test-1").state(VolumeState.IN_USE).build();
                    return DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
                }
                software.amazon.awssdk.services.ec2.model.Volume volumeResponse = software.amazon.awssdk.services.ec2.model.Volume.builder()
                        .volumeId("vol-test-1").state(VolumeState.AVAILABLE).build();
                return DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
            }
        }).when(amazonEc2Client).describeVolumes(any());

        underTest.detachVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    void testDetachVolumesException() throws Exception {
        doReturn(amazonEc2Client).when(authenticatedContext).getParameter(any());
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
    void testDeleteVolumes() throws Exception {
        doReturn(amazonEc2Client).when(authenticatedContext).getParameter(any());
        CloudResource cloudResource = mock(CloudResource.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn("TEST-1").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn("vol-test-1").when(volume).getId();
        underTest.deleteVolumes(authenticatedContext, List.of(cloudResource));
    }

    @Test
    void testPollVolumeStates() {
        List<String> volIdsToPoll = List.of("vol-1");
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder().volumes(Volume.builder().state(VolumeState.AVAILABLE)
                .build()).build();
        doReturn(describeVolumesResponse).when(amazonEc2Client).describeVolumes(any());
        underTest.pollVolumeStates(amazonEc2Client, volIdsToPoll);
        verify(amazonEc2Client, times(1)).describeVolumes(any());
    }

    @Test
    void testPollVolumeStatesPollingCalledTwice() {
        List<String> volIdsToPoll = List.of("vol-1");
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder().volumes(Volume.builder().state(VolumeState.IN_USE)
                .build()).build();
        DescribeVolumesResponse describeVolumesResponse1 = DescribeVolumesResponse.builder().volumes(Volume.builder().state(VolumeState.AVAILABLE)
                .build()).build();
        doReturn(describeVolumesResponse).doReturn(describeVolumesResponse1).when(amazonEc2Client).describeVolumes(any());
        underTest.pollVolumeStates(amazonEc2Client, volIdsToPoll);
        verify(amazonEc2Client, times(2)).describeVolumes(any());
    }

    @Test
    void testPollVolumeStatesPollingException() {
        List<String> volIdsToPoll = List.of("vol-1");
        doThrow(AwsServiceException.builder().message("Test").build()).when(amazonEc2Client).describeVolumes(any());
        PollerStoppedException exception = assertThrows(PollerStoppedException.class,
                () -> underTest.pollVolumeStates(amazonEc2Client, volIdsToPoll));
        assertEquals("com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException: Exception while querying the volume states for volumes " +
                "- [vol-1]. returning empty map. Exception - Test", exception.getMessage());
    }

    @Test
    void testGetVolumeStates() {
        List<String> volIdsToPoll = List.of("vol-1");
        software.amazon.awssdk.services.ec2.model.Volume volume = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .volumeId("vol-1").state(VolumeState.AVAILABLE).build();
        doReturn(DescribeVolumesResponse.builder().volumes(volume).build()).when(amazonEc2Client).describeVolumes(any());
        Map<String, VolumeState> volumeStates = underTest.getVolumeStates(volIdsToPoll, amazonEc2Client, Map.of());
        assertEquals(VolumeState.AVAILABLE, volumeStates.get("vol-1"));
    }

    @Test
    void testGetVolumeStatesWithTagFilter() {
        Map<String, List<String>> filterInput = Map.of("tag:created-for", List.of("x1.com"));
        software.amazon.awssdk.services.ec2.model.Volume volume = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .volumeId("vol-1").state(VolumeState.AVAILABLE).build();
        doReturn(DescribeVolumesResponse.builder().volumes(volume).build()).when(amazonEc2Client).describeVolumes(any());
        Map<String, VolumeState> volumeStates = underTest.getVolumeStates(List.of(), amazonEc2Client, filterInput);
        assertEquals(VolumeState.AVAILABLE, volumeStates.get("vol-1"));
    }

    @Test
    void testGetVolumeStatesNoVolumesReturned() {
        List<String> volIdsToPoll = List.of("vol-1");
        doReturn(DescribeVolumesResponse.builder().build()).when(amazonEc2Client).describeVolumes(any());
        Map<String, VolumeState> volumeStates = underTest.getVolumeStates(volIdsToPoll, amazonEc2Client, Map.of());
        Map<String, VolumeState> expectedVolumeResponse = Map.of("", VolumeState.UNKNOWN_TO_SDK_VERSION);
        assertEquals(expectedVolumeResponse, volumeStates);
    }

    @Test
    void getVolumesInAvailableStatusByTagsFilter() {
        Map<String, List<String>> filterInput = Map.of("tag:created-for", List.of("x1.com"));
        software.amazon.awssdk.services.ec2.model.Volume volume = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .volumeId("vol-1").state(VolumeState.AVAILABLE).tags(Tag.builder().key("created-for").value("x1.com").build()).build();
        doReturn(DescribeVolumesResponse.builder().volumes(volume).build()).when(amazonEc2Client).describeVolumes(any());
        Map<String, List<software.amazon.awssdk.services.ec2.model.Volume>> volumesMap =
                underTest.getVolumesInAvailableStatusByTagsFilter(amazonEc2Client, filterInput);
        assertTrue(volumesMap.containsKey("x1.com"));
        assertEquals(1, volumesMap.get("x1.com").size());
        assertEquals(VolumeState.AVAILABLE, volumesMap.get("x1.com").get(0).state());
        assertEquals("vol-1", volumesMap.get("x1.com").get(0).volumeId());
    }
}