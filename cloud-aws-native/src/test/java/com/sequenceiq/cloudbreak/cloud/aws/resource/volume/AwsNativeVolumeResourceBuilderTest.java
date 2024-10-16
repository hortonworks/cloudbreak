package com.sequenceiq.cloudbreak.cloud.aws.resource.volume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.VolumeResourceCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.Architecture;

import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.InstanceStatusSummary;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeState;

@ExtendWith(MockitoExtension.class)
class AwsNativeVolumeResourceBuilderTest {

    @InjectMocks
    private AwsNativeVolumeResourceBuilder underTest;

    @Mock
    private AwsContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private VolumeResourceCollector volumeResourceCollector;

    @Mock
    private AmazonEc2Client client;

    @Test
    void checkResourcesWhenArm64Polling() {
        List<CloudResource> cloudResources = List.of(CloudResource.builder()
                .withType(ResourceType.AWS_VOLUMESET)
                .withStatus(CommonStatus.REQUESTED)
                .withName("inst01_vs01")
                .withParameters(Map.of(CloudResource.ARCHITECTURE, Architecture.ARM64.name())).build());
        Pair<List<String>, List<CloudResource>> pair = Pair.of(List.of("vol01"), cloudResources);
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(eq(cloudResources), eq(ResourceType.AWS_VOLUMESET), any())).thenReturn(pair);

        when(context.getAmazonEc2Client()).thenReturn(client);
        when(commonAwsClient.createEc2Client(eq(auth))).thenReturn(client);
        when(client.describeVolumes(any(DescribeVolumesRequest.class))).thenReturn(
                DescribeVolumesResponse.builder().volumes(
                        Volume.builder().volumeId("vol01").state(VolumeState.AVAILABLE).build()
                ).build());
        when(client.describeInstanceStatus(any(DescribeInstanceStatusRequest.class))).thenReturn(
                DescribeInstanceStatusResponse.builder()
                        .instanceStatuses(
                                InstanceStatus.builder()
                                        .instanceId("inst01")
                                        .systemStatus(InstanceStatusSummary.builder()
                                                .status(SummaryStatus.INITIALIZING)
                                                .build())
                                        .build()
                        )
                        .build()
        ).thenReturn(
                DescribeInstanceStatusResponse.builder()
                        .instanceStatuses(
                                InstanceStatus.builder()
                                        .instanceId("inst01")
                                        .systemStatus(InstanceStatusSummary.builder()
                                                .status(SummaryStatus.OK)
                                                .build())
                                        .build()
                        )
                        .build()
        );
        List<CloudResourceStatus> crs1 = underTest.checkResources(ResourceType.AWS_VOLUMESET, context, auth, cloudResources);
        assertEquals(ResourceStatus.IN_PROGRESS, crs1.getFirst().getStatus());
        List<CloudResourceStatus> crs2 = underTest.checkResources(ResourceType.AWS_VOLUMESET, context, auth, cloudResources);
        assertEquals(ResourceStatus.CREATED, crs2.getFirst().getStatus());
    }

    @Test
    void checkResourcesWhenx86NoPolling() {
        List<CloudResource> cloudResources = List.of(CloudResource.builder()
                .withType(ResourceType.AWS_VOLUMESET)
                .withStatus(CommonStatus.REQUESTED)
                .withName("inst01_vs01")
                .withParameters(Map.of(CloudResource.ARCHITECTURE, Architecture.X86_64.name())).build());
        Pair<List<String>, List<CloudResource>> pair = Pair.of(List.of("vol01"), cloudResources);
        when(volumeResourceCollector.getVolumeIdsByVolumeResources(eq(cloudResources), eq(ResourceType.AWS_VOLUMESET), any())).thenReturn(pair);

        when(commonAwsClient.createEc2Client(eq(auth))).thenReturn(client);
        when(client.describeVolumes(any(DescribeVolumesRequest.class))).thenReturn(
                DescribeVolumesResponse.builder().volumes(
                        Volume.builder().volumeId("vol01").state(VolumeState.AVAILABLE).build()
                ).build());
        List<CloudResourceStatus> crs1 = underTest.checkResources(ResourceType.AWS_VOLUMESET, context, auth, cloudResources);
        assertEquals(ResourceStatus.CREATED, crs1.getFirst().getStatus());
        verify(client, never()).describeInstanceStatus(any());
    }
}