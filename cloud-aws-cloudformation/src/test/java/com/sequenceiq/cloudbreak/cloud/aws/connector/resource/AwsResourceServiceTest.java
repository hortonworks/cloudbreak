package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstaceStorageInfo;

import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DiskInfo;
import software.amazon.awssdk.services.ec2.model.InstanceStorageInfo;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;

@ExtendWith(MockitoExtension.class)
public class AwsResourceServiceTest {

    @Mock
    private AwsCloudFormationClient awsClient;

    @InjectMocks
    private AwsResourceService underTest;

    private final AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);

    private final AmazonEc2Client amazonEc2Client = mock(AmazonEc2Client.class);

    @BeforeEach
    public void setUp() {
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Region region = mock(Region.class);
        doReturn(cloudContext).when(authenticatedContext).getCloudContext();
        doReturn(location).when(cloudContext).getLocation();
        doReturn(region).when(location).getRegion();
        doReturn("").when(region).value();
        doReturn(mock(CloudCredential.class)).when(authenticatedContext).getCloudCredential();
        doReturn(amazonEc2Client).when(awsClient).createEc2Client(any(AwsCredentialView.class), any(String.class));
    }

    @Test
    void testGetInstanceTypeEphemeralInfoForM5ad() {
        DiskInfo diskInfo = DiskInfo.builder().type("Ephemeral").count(1).sizeInGB(100L).build();
        InstanceStorageInfo instanceStorageInfo = InstanceStorageInfo.builder().disks(diskInfo).build();
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder().instanceType("m5ad.2xlarge")
                .instanceStorageSupported(true).instanceStorageInfo(instanceStorageInfo).build();
        DescribeInstanceTypesResponse instanceTypesResult = DescribeInstanceTypesResponse.builder()
                .instanceTypes(instanceTypeInfo).build();
        doReturn(instanceTypesResult).when(amazonEc2Client).describeInstanceTypes(any(DescribeInstanceTypesRequest.class));
        List<AwsInstaceStorageInfo> results = underTest.getInstanceTypeEphemeralInfo(authenticatedContext, List.of("m5ad.2xlarge"));
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getInstanceStorageCount());
        assertEquals(100, results.get(0).getInstanceStorageSize());
        assertTrue(results.get(0).isInstanceStorageSupport());
    }

    @Test
    void testGetInstanceTypeEphemeralInfoForM5d() {
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder().instanceType("m5d.2xlarge")
                .instanceStorageSupported(false).build();
        DescribeInstanceTypesResponse instanceTypesResult = DescribeInstanceTypesResponse.builder()
                .instanceTypes(instanceTypeInfo).build();
        doReturn(instanceTypesResult).when(amazonEc2Client).describeInstanceTypes(any(DescribeInstanceTypesRequest.class));
        List<AwsInstaceStorageInfo> results = underTest.getInstanceTypeEphemeralInfo(authenticatedContext, List.of("m5ad.2xlarge"));
        assertEquals(results.size(), 0);
    }

    @Test
    void testGetInstanceTypeEphemeralInfoThrowsException() {
        doThrow(new RuntimeException("Connection Refused")).when(amazonEc2Client).describeInstanceTypes(any(DescribeInstanceTypesRequest.class));
        assertThrows(RuntimeException.class, () -> underTest.getInstanceTypeEphemeralInfo(authenticatedContext, List.of("m5ad.2xlarge")));
    }
}
