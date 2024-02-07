package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.AttachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@ExtendWith(MockitoExtension.class)
class AwsAdditionalDiskAttachmentServiceTest {

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @InjectMocks
    private AwsAdditionalDiskAttachmentService underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private AmazonEc2Client client;

    @Mock
    private VolumeSetAttributes volumeSetAttributes;

    @Mock
    private VolumeSetAttributes.Volume volume;

    @Mock
    private AttachVolumeResponse attachVolumeResponse;

    @BeforeEach
    void setUp() {
        doReturn(client).when(commonAwsClient).createEc2Client(authenticatedContext);
        doReturn("test-instance-id").when(cloudResource).getInstanceId();
        doReturn("vol-id").when(volume).getId();
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Test
    void testAttachDisks() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn(attachVolumeResponse);
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        doReturn(Map.of("vol-id", VolumeState.AVAILABLE)).when(awsCommonDiskUpdateService).getVolumeStates(List.of("vol-id"), client, Map.of());
        ArgumentCaptor<Callable> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        ArgumentCaptor<AttachVolumeRequest> attachRequestCaptor = ArgumentCaptor.forClass(AttachVolumeRequest.class);
        underTest.attachAllVolumes(authenticatedContext, List.of(cloudResource));
        verify(intermediateBuilderExecutor).submit(callableCaptor.capture());
        callableCaptor.getValue().call();
        verify(client).attachVolume(attachRequestCaptor.capture());
        assertEquals("test-instance-id", attachRequestCaptor.getValue().instanceId());
        assertEquals("vol-id", attachRequestCaptor.getValue().volumeId());
    }

    @Test
    void testAttachDisksUnavailableDisk() {
        doReturn(Map.of("vol-id", VolumeState.IN_USE)).when(awsCommonDiskUpdateService).getVolumeStates(List.of("vol-id"), client, Map.of());
        underTest.attachAllVolumes(authenticatedContext, List.of(cloudResource));
        verify(client, times(0)).attachVolume(any());
        verify(awsCommonDiskUpdateService, times(1)).getVolumeStates(List.of("vol-id"), client, Map.of());
    }

    @Test
    void testAttachDisksThrowsException() throws Exception {
        Future future = mock(Future.class);
        when(future.get()).thenReturn(attachVolumeResponse);
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        ArgumentCaptor<Callable> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        doReturn(Map.of("vol-id", VolumeState.AVAILABLE)).when(awsCommonDiskUpdateService).getVolumeStates(List.of("vol-id"), client, Map.of());
        doThrow(new CloudbreakServiceException("TEST")).when(client).attachVolume(any());
        underTest.attachAllVolumes(authenticatedContext, List.of(cloudResource));
        verify(intermediateBuilderExecutor).submit(callableCaptor.capture());
        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () -> callableCaptor.getValue().call());
        assertEquals("TEST", ex.getMessage());
    }

    @Test
    void testAttachDisksThrowsExceptionWhenNotAllVolumesAttached() throws Exception {
        DescribeVolumesResponse describeVolumesResponse = mock(DescribeVolumesResponse.class);
        doReturn(List.of()).when(describeVolumesResponse).volumes();
        doReturn(describeVolumesResponse).when(client).describeVolumes(any(DescribeVolumesRequest.class));
        Future future = mock(Future.class);
        doThrow(new RuntimeException("Test")).when(future).get();
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        ArgumentCaptor<Callable> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        doReturn(Map.of("vol-id", VolumeState.AVAILABLE)).when(awsCommonDiskUpdateService).getVolumeStates(List.of("vol-id"), client, Map.of());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.attachAllVolumes(authenticatedContext, List.of(cloudResource)));
        verify(intermediateBuilderExecutor).submit(callableCaptor.capture());
        assertEquals("Some Volume attachment were unsuccessful. Test", exception.getMessage());
    }

    @Test
    void testAttachDisksDoesNotThrowExceptionWhenAllVolumesAttached() throws Exception {
        Volume volumeResponse = Volume.builder().volumeId("vol-id").build();
        DescribeVolumesResponse describeVolumesResponse = mock(DescribeVolumesResponse.class);
        doReturn(List.of(volumeResponse)).when(describeVolumesResponse).volumes();
        doReturn(describeVolumesResponse).when(client).describeVolumes(any(DescribeVolumesRequest.class));
        ArgumentCaptor<AttachVolumeRequest> attachRequestCaptor = ArgumentCaptor.forClass(AttachVolumeRequest.class);
        Future future = mock(Future.class);
        doThrow(new RuntimeException("Test")).when(future).get();
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenReturn(future);
        ArgumentCaptor<Callable> callableCaptor = ArgumentCaptor.forClass(Callable.class);
        doReturn(Map.of("vol-id", VolumeState.AVAILABLE)).when(awsCommonDiskUpdateService).getVolumeStates(List.of("vol-id"), client, Map.of());
        underTest.attachAllVolumes(authenticatedContext, List.of(cloudResource));
        verify(intermediateBuilderExecutor).submit(callableCaptor.capture());
        callableCaptor.getValue().call();
        verify(client).attachVolume(attachRequestCaptor.capture());
        assertEquals("test-instance-id", attachRequestCaptor.getValue().instanceId());
        assertEquals("vol-id", attachRequestCaptor.getValue().volumeId());
    }
}
