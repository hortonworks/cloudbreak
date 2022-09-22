package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

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
        when(ec2Client.describeVolumes(any())).thenReturn(DescribeVolumesResponse.builder()
                .volumes(Volume.builder().volumeType("gp2").build())
                .build());

        Optional<DescribeVolumesResponse> ebsSize = underTest.getEbsSize(cloudCredential, "region", "ebs");

        assertTrue(ebsSize.isPresent());
    }
}
