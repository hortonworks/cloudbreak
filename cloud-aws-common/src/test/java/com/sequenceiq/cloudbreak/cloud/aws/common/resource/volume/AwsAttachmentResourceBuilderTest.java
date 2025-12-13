package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@ExtendWith(MockitoExtension.class)
class AwsAttachmentResourceBuilderTest {

    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;

    @Spy
    private AwsInstanceFinder awsInstanceFinder;

    @Mock
    private VolumeResourceCollector volumeResourceCollector;

    @Mock
    private CommonAwsClient commonAwsClient;

    @InjectMocks
    private AwsAttachmentResourceBuilder awsAttachmentResourceBuilder;

    @Test
    void testBuildIfFutureGetFailsButVolumesAreSuccessfullyAttached() throws Exception {
        List<CloudResource> buildableResource = new ArrayList<>();
        buildableResource.add(CloudResource.builder().withName("instance").withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").build());
        List<VolumeSetAttributes.Volume> volumes = List.of(new VolumeSetAttributes.Volume("vol1", "device", 50, "type", CloudVolumeUsageType.GENERAL),
                new VolumeSetAttributes.Volume("vol2", "device", 50, "type", CloudVolumeUsageType.GENERAL));
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("", false, "", volumes, 50, "type");
        CloudResource volumeResource = CloudResource.builder().withName("vol1").withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").withParameters(Map.of(ATTRIBUTES, volumeSetAttributes)).build();
        buildableResource.add(volumeResource);
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        DescribeVolumesResponse describeVolumesResult = DescribeVolumesResponse.builder()
                .volumes(Volume.builder().volumeId("vol1").build(), Volume.builder().volumeId("vol2").build())
                .build();
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResult);
        when(commonAwsClient.createEc2Client(any(AuthenticatedContext.class))).thenReturn(amazonEc2Client);
        Future future = mock(Future.class);
        when(future.get()).thenThrow(new RuntimeException("error"));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenReturn(future);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        AwsContext awsContext = mock(AwsContext.class);
        when(awsContext.getComputeResources(1L)).thenReturn(buildableResource);
        awsAttachmentResourceBuilder.build(awsContext, null, 1L, authenticatedContext, mock(Group.class), buildableResource, null);
    }

    @Test
    void testBuildIfFutureGetFailsButOneVolumeAttachmentFailed() throws Exception {
        List<CloudResource> buildableResource = new ArrayList<>();
        buildableResource.add(CloudResource.builder().withName("instance").withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").build());
        List<VolumeSetAttributes.Volume> volumes = List.of(new VolumeSetAttributes.Volume("vol1", "device", 50, "type", CloudVolumeUsageType.GENERAL),
                new VolumeSetAttributes.Volume("vol2", "device", 50, "type", CloudVolumeUsageType.GENERAL));
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("", false, "", volumes, 50, "type");
        CloudResource volumeResource = CloudResource.builder().withName("vol1").withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").withParameters(Map.of(ATTRIBUTES, volumeSetAttributes)).build();
        buildableResource.add(volumeResource);
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        DescribeVolumesResponse describeVolumesResult = DescribeVolumesResponse.builder()
                .volumes(Volume.builder().volumeId("vol1").build())
                .build();
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResult);
        when(commonAwsClient.createEc2Client(any(AuthenticatedContext.class))).thenReturn(amazonEc2Client);
        Future future = mock(Future.class);
        String errorMessage = "Error during attaching disks.";
        when(future.get()).thenThrow(new RuntimeException(errorMessage));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenReturn(future);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        AwsContext awsContext = mock(AwsContext.class);
        when(awsContext.getComputeResources(1L)).thenReturn(buildableResource);
        CloudbreakServiceException runtimeException = assertThrows(CloudbreakServiceException.class,
                () -> awsAttachmentResourceBuilder.build(awsContext, null, 1L, authenticatedContext,
                        mock(Group.class), buildableResource, null));
        assertEquals("Volume attachment were unsuccessful. " + errorMessage, runtimeException.getMessage());
    }

    @Test
    public void testBuildIfFutureGetFailsButOneVolumeAttachmentFailedWithInstanceIsNotRunningError() throws Exception {
        List<CloudResource> buildableResource = new ArrayList<>();
        buildableResource.add(CloudResource.builder().withName("instance").withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").build());
        List<VolumeSetAttributes.Volume> volumes = List.of(new VolumeSetAttributes.Volume("vol1", "device", 50, "type", CloudVolumeUsageType.GENERAL),
                new VolumeSetAttributes.Volume("vol2", "device", 50, "type", CloudVolumeUsageType.GENERAL));
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("", false, "", volumes, 50, "type");
        CloudResource volumeResource = CloudResource.builder().withName("vol1").withType(ResourceType.AWS_VOLUMESET).withStatus(CommonStatus.CREATED)
                .withInstanceId("instanceid").withParameters(Map.of(ATTRIBUTES, volumeSetAttributes)).build();
        buildableResource.add(volumeResource);
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        DescribeVolumesResponse describeVolumesResult = DescribeVolumesResponse.builder()
                .volumes(Volume.builder().volumeId("vol1").build())
                .build();
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResult);
        when(commonAwsClient.createEc2Client(any(AuthenticatedContext.class))).thenReturn(amazonEc2Client);
        Future future = mock(Future.class);
        String errorMessage = "Instance is not 'running'";
        when(future.get()).thenThrow(new RuntimeException(errorMessage));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenReturn(future);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        AwsContext awsContext = mock(AwsContext.class);
        when(awsContext.getComputeResources(1L)).thenReturn(buildableResource);
        CloudbreakServiceException runtimeException = assertThrows(CloudbreakServiceException.class,
                () -> awsAttachmentResourceBuilder.build(awsContext, null, 1L, authenticatedContext,
                        mock(Group.class), buildableResource, null));
        assertEquals("Volume attachment were unsuccessful. The related instance is not available. Usually this happens when an AWS policy " +
                "terminates the instance, or because of a quota issue. Please check the AWS console! " + errorMessage, runtimeException.getMessage());
    }

}
