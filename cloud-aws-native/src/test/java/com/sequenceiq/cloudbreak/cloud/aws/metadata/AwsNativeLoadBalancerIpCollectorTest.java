package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerIpCollectorTest {

    private static final String LB_PRIVATE_IP_1 = "1.1.1.1";

    private static final String LB_PRIVATE_IP_2 = "2.2.2.2";

    private static final String LB_NAME = "lb-name";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:Test:freeipa:b3a8d7d4-2bb6-4ec0-8889-1bd525c5ab74";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:Test:datalake:40231209-6037-4d4b-95b9-f3c30698ae98";

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

        Optional<String> actual = doAs(USER_CRN, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME, FREEIPA_CRN));

        assertTrue(actual.isPresent());
        assertEquals(String.join(",", List.of(LB_PRIVATE_IP_1, LB_PRIVATE_IP_2)), actual.get());
    }

    @Test
    void testGetLoadBalancerIpShouldReturnOptionalEmptyWhenTheStackIsDataLake() {
        assertTrue(doAs(USER_CRN, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME, DATALAKE_CRN)).isEmpty());
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponseIsNull() {
        when(ec2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder().build());

        assertThrows(NotFoundException.class, () -> doAs(USER_CRN, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME, FREEIPA_CRN)));
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponsePrivateIpIsNull() {
        when(ec2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(NetworkInterface.builder().build(), NetworkInterface.builder().build())
                .build());

        assertThrows(NotFoundException.class, () -> doAs(USER_CRN, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME, FREEIPA_CRN)));
    }

    @Test
    void testGetLoadBalancerIpShouldThrowCloudConnectorExceptionWhenThereIsNoPermission() {
        doThrow(Ec2Exception.builder().message("No permission").build()).when(ec2Client).describeNetworkInterfaces(any());

        assertThrows(CloudConnectorException.class, () -> doAs(USER_CRN, () -> underTest.getLoadBalancerIp(ec2Client, LB_NAME, FREEIPA_CRN)));
    }

}