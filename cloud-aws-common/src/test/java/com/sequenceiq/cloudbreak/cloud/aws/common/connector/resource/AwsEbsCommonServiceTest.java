package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class AwsEbsCommonServiceTest {

    @Mock
    private CommonAwsClient awsClient;

    @InjectMocks
    private AwsEbsCommonService underTest;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AmazonEc2Client ec2Client;

    @Test
    public void getEbsSize() {
        when(awsClient.createAccessWithMinimalRetries(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVolumes(any())).thenReturn(new DescribeVolumesResult()
                .withVolumes(new Volume().withVolumeType("gp2")));

        Optional<DescribeVolumesResult> ebsSize = underTest.getEbsSize(cloudCredential, "region", "ebs");

        assertEquals(ebsSize.isPresent(), true);
    }
}