package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class AwsInstanceCommonServiceTest {

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private DescribeInstancesResult describeInstancesResult;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private AwsInstanceCommonService underTest;

    @Test
    public void getAttachedVolumes() {
        Reservation reservation = new Reservation()
                .withReservationId("1")
                .withInstances(new Instance()
                        .withBlockDeviceMappings(new InstanceBlockDeviceMapping()
                                .withEbs(new EbsInstanceBlockDevice().withVolumeId("volt-123"))));

        when(awsClient.createAccessWithMinimalRetries(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult);
        when(describeInstancesResult.getReservations()).thenReturn(List.of(reservation));

        Set<String> result = underTest.getAttachedVolumes(cloudCredential, "region", "1");

        assertEquals(result.stream().findFirst().get(), "volt-123");
        assertEquals(result.size(), 1);
    }
}