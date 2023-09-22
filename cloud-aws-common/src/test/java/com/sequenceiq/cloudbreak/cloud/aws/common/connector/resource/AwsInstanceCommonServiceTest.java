package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;

import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Reservation;

@ExtendWith(MockitoExtension.class)
public class AwsInstanceCommonServiceTest {

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private AwsInstanceCommonService underTest;

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
}