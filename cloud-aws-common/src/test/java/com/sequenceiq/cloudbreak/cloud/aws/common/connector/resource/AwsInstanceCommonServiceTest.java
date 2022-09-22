package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

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
    public void getAttachedVolumes() {
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
}