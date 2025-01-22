package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerIpCollectorTest {

    private static final String LB_PRIVATE_IP_1 = "1.1.1.1";

    private static final String LB_PRIVATE_IP_2 = "2.2.2.2";

    private static final String LB_NAME = "lb-name";

    @InjectMocks
    private AwsNativeLoadBalancerIpCollector underTest;

    @Mock
    private AmazonEc2Client ec2Client;

    @Test
    void testGetLoadBalancerIpShouldReturnLoadBalancerIpAddresses() {
        when(ec2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(NetworkInterface.builder().privateIpAddress(LB_PRIVATE_IP_1).build(),
                        NetworkInterface.builder().privateIpAddress(LB_PRIVATE_IP_2).build())
                .build());

        String actual = underTest.getLoadBalancerIp(ec2Client, LB_NAME);

        assertEquals(String.join(",", List.of(LB_PRIVATE_IP_1, LB_PRIVATE_IP_2)), actual);
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponseIsNull() {
        when(ec2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder().build());

        assertThrows(NotFoundException.class, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME));
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponsePrivateIpIsNull() {
        when(ec2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(NetworkInterface.builder().build(), NetworkInterface.builder().build())
                .build());

        assertThrows(NotFoundException.class, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME));
    }

}