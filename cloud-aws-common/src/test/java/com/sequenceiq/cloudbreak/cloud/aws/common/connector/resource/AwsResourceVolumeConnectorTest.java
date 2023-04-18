package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@ExtendWith(MockitoExtension.class)
public class AwsResourceVolumeConnectorTest {

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private VolumeSetAttributes volumeSetAttributes;

    @Mock
    private VolumeSetAttributes.Volume volume;

    @InjectMocks
    @Spy
    private AwsResourceVolumeConnector underTest;

    @Mock
    private AuthenticatedContextView authenticatedContextView;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @BeforeEach
    public void setUp() {
        doReturn("TEST-1").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        doReturn(List.of(volume)).when(volumeSetAttributes).getVolumes();
        doReturn("vol-test-1").when(volume).getId();
        doReturn(amazonEc2Client).when(underTest).getEc2Client(authenticatedContext);
    }

    @Test
    public void testDetachVolumes() throws Exception {
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
        software.amazon.awssdk.services.ec2.model.Volume volumeResponse = software.amazon.awssdk.services.ec2.model.Volume.builder()
                .state(VolumeState.IN_USE).build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(List.of(volumeResponse)).build();
        doReturn(response).when(amazonEc2Client).describeVolumes(any());
        doThrow(AwsServiceException.builder().message("TEST").build()).when(amazonEc2Client).detachVolume(any());
        AwsServiceException exception = assertThrows(AwsServiceException.class, () -> underTest.detachVolumes(authenticatedContext, List.of(cloudResource)));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void testDeleteVolumes() throws Exception {
        underTest.deleteVolumes(authenticatedContext, List.of(cloudResource));
    }
}
