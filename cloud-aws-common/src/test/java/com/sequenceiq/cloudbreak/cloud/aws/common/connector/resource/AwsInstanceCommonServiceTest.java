package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;

@ExtendWith(MockitoExtension.class)
class AwsInstanceCommonServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private AwsInstanceCommonService underTest;

    @Captor
    private ArgumentCaptor<DescribeInstancesRequest> describeInstancesRequestCaptor;

    @Test
    void getAttachedVolumes() {
        Reservation reservation = Reservation.builder()
                .reservationId("1")
                .instances(Instance.builder()
                        .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                                .ebs(EbsInstanceBlockDevice.builder().volumeId("volt-123").build())
                                .build())
                        .build())
                .build();
        DescribeInstancesResponse describeInstancesResult = DescribeInstancesResponse.builder()
                .reservations(reservation).build();

        when(awsClient.createAccessWithMinimalRetries(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult);

        Set<String> result = underTest.getAttachedVolumes(cloudCredential, "region", "1");

        assertEquals(result.stream().findFirst().get(), "volt-123");
        assertEquals(result.size(), 1);
    }

    @Test
    void testCollectInstanceTypes() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        when(ac.getParameter(any(Class.class))).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder().instanceId("instance1").instanceType("large").build(),
                                Instance.builder().instanceId("instance2").instanceType("large").build())
                        .build())
                .build());
        InstanceTypeMetadata result = underTest.collectInstanceTypes(ac, List.of("instance1", "instance2"));

        Map<String, String> instanceTypes = result.getInstanceTypes();
        verify(amazonEc2Client, times(1)).describeInstances(any());
        assertThat(instanceTypes).hasSize(2);
        assertThat(instanceTypes).containsEntry("instance1", "large");
        assertThat(instanceTypes).containsEntry("instance2", "large");
    }

    @Test
    void testCollectCdpInstances() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withLocation(Location.location(Region.region("region")))
                .build();
        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                        .instanceId("instance1")
                                        .instanceType("r4.large")
                                        .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                        .build(),
                                Instance.builder()
                                        .instanceId("instance2")
                                        .instanceType("r5.4xlarge")
                                        .state(InstanceState.builder().name(InstanceStateName.PENDING).build())
                                        .build())
                        .build())
                .build();
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("region"))).thenReturn(ec2Client);
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(ac, RESOURCE_CRN, List.of());

        verify(ec2Client).describeInstances(describeInstancesRequestCaptor.capture());
        List<Filter> filters = describeInstancesRequestCaptor.getValue().filters();
        assertEquals(1, filters.size());
        assertEquals("tag:Cloudera-Resource-Name", filters.getFirst().name());
        assertEquals(RESOURCE_CRN, filters.getFirst().values().getFirst());
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder("instance1", "instance2");
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("r4.large", "r5.4xlarge");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED, InstanceStatus.IN_PROGRESS);
    }

    @Test
    void testCollectCdpInstancesWhenThereIsAKnownInstanceIdMissing() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withLocation(Location.location(Region.region("region")))
                .build();
        DescribeInstancesResponse describeInstancesResponse1 = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .instanceId("instance1")
                                .instanceType("r4.large")
                                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                .build())
                        .build())
                .build();
        DescribeInstancesResponse describeInstancesResponse2 = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .instanceId("instance2")
                                .instanceType("r5.4xlarge")
                                .state(InstanceState.builder().name(InstanceStateName.PENDING).build())
                                .build())
                        .build())
                .build();
        AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(ac.getCloudCredential()).thenReturn(cloudCredential);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("region"))).thenReturn(ec2Client);
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(describeInstancesResponse1)
                .thenReturn(describeInstancesResponse2);

        List<InstanceCheckMetadata> result = underTest.collectCdpInstances(ac, RESOURCE_CRN, List.of("instance2"));

        verify(ec2Client, times(2)).describeInstances(describeInstancesRequestCaptor.capture());
        List<Filter> filters1 = describeInstancesRequestCaptor.getAllValues().get(0).filters();
        assertEquals(1, filters1.size());
        assertEquals("tag:Cloudera-Resource-Name", filters1.getFirst().name());
        assertEquals(RESOURCE_CRN, filters1.getFirst().values().getFirst());
        List<Filter> filters2 = describeInstancesRequestCaptor.getAllValues().get(1).filters();
        assertEquals(0, filters2.size());
        assertThat(describeInstancesRequestCaptor.getAllValues().get(1).instanceIds()).containsExactlyInAnyOrder("instance2");
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InstanceCheckMetadata::instanceId).containsExactlyInAnyOrder("instance1", "instance2");
        assertThat(result).extracting(InstanceCheckMetadata::instanceType).containsExactlyInAnyOrder("r4.large", "r5.4xlarge");
        assertThat(result).extracting(InstanceCheckMetadata::status).containsExactlyInAnyOrder(InstanceStatus.STARTED, InstanceStatus.IN_PROGRESS);
    }
}