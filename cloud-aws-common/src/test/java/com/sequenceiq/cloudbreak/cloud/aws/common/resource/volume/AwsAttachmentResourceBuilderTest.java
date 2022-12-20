package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AwsAttachmentResourceBuilderTest {

    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private VolumeResourceCollector volumeResourceCollector;

    @InjectMocks
    private AwsAttachmentResourceBuilder awsAttachmentResourceBuilder;

    @Test
    public void testBuildIfFutureGetFailsButVolumesAreSuccessfullyAttached() throws Exception {
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
        DescribeVolumesResult describeVolumesResult = new DescribeVolumesResult();
        describeVolumesResult.setVolumes(List.of(new Volume().withVolumeId("vol1"), new Volume().withVolumeId("vol2")));
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResult);
        when(awsClient.createEc2Client(any(), any())).thenReturn(amazonEc2Client);
        Future future = mock(Future.class);
        when(future.get()).thenThrow(new RuntimeException("error"));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenReturn(future);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("eu"), AvailabilityZone.availabilityZone("az1")));
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        awsAttachmentResourceBuilder.build(mock(AwsContext.class), null, 1L, authenticatedContext, mock(Group.class), buildableResource, null);
    }

    @Test
    public void testBuildIfFutureGetFailsButOneVolumeAttachmentFailed() throws Exception {
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
        DescribeVolumesResult describeVolumesResult = new DescribeVolumesResult();
        describeVolumesResult.setVolumes(List.of(new Volume().withVolumeId("vol1")));
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResult);
        when(awsClient.createEc2Client(any(), any())).thenReturn(amazonEc2Client);
        Future future = mock(Future.class);
        String errorMessage = "Error during attaching disks.";
        when(future.get()).thenThrow(new RuntimeException(errorMessage));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenReturn(future);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("eu"), AvailabilityZone.availabilityZone("az1")));
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        CloudbreakServiceException runtimeException = Assertions.assertThrows(CloudbreakServiceException.class,
                () -> awsAttachmentResourceBuilder.build(mock(AwsContext.class), null, 1L, authenticatedContext,
                mock(Group.class), buildableResource, null));
        assertEquals("Volume attachment were unsuccessful. " + errorMessage, runtimeException.getMessage());
    }

}
