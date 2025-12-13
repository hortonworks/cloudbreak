package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsSubnetRequestProviderTest {

    private static final String CIDR_1 = "1.1.1.1/24";

    private static final String CIDR_2 = "2.2.2.2/24";

    private static final String CIDR_3 = "3.3.3.3/24";

    private static final String CIDR_4 = "4.4.4.4/24";

    private static final String CIDR_5 = "5.5.5.5/24";

    private static final String CIDR_6 = "6.6.6.6/24";

    private static final String AZ_1 = "London";

    private static final String AZ_2 = "Bristol";

    private static final String AZ_3 = "Manchester";

    private static final String AZ_4 = "Oxford";

    @InjectMocks
    private AwsSubnetRequestProvider underTest;

    @Test
    public void testProvideWhenTwoAzAvailable() {
        AmazonEc2Client ec2Client = createEc2Client(List.of(createAZ(AZ_1), createAZ(AZ_2)));
        List<NetworkSubnetRequest> publicSubnets = List.of(createSubnetRequest(CIDR_4), createSubnetRequest(CIDR_5), createSubnetRequest(CIDR_6));
        List<NetworkSubnetRequest> privateSubnets = List.of(createSubnetRequest(CIDR_1), createSubnetRequest(CIDR_2), createSubnetRequest(CIDR_3));

        List<SubnetRequest> actual = underTest.provide(ec2Client, publicSubnets, privateSubnets);

        assertEquals(CIDR_4, actual.get(0).getPublicSubnetCidr());
        assertEquals(AZ_1, actual.get(0).getAvailabilityZone());
        assertEquals(CIDR_5, actual.get(1).getPublicSubnetCidr());
        assertEquals(AZ_2, actual.get(1).getAvailabilityZone());
        assertEquals(CIDR_6, actual.get(2).getPublicSubnetCidr());
        assertEquals(AZ_1, actual.get(2).getAvailabilityZone());

        assertEquals(CIDR_1, actual.get(3).getPrivateSubnetCidr());
        assertEquals(AZ_1, actual.get(3).getAvailabilityZone());
        assertEquals(CIDR_2, actual.get(4).getPrivateSubnetCidr());
        assertEquals(AZ_2, actual.get(4).getAvailabilityZone());
        assertEquals(CIDR_3, actual.get(5).getPrivateSubnetCidr());
        assertEquals(AZ_1, actual.get(5).getAvailabilityZone());
    }

    @Test
    public void testProvideWhenFourAzAvailable() {
        AmazonEc2Client ec2Client = createEc2Client(List.of(createAZ(AZ_1), createAZ(AZ_2), createAZ(AZ_3), createAZ(AZ_4)));
        List<NetworkSubnetRequest> publicSubnets = List.of(createSubnetRequest(CIDR_4), createSubnetRequest(CIDR_5), createSubnetRequest(CIDR_6));
        List<NetworkSubnetRequest> privateSubnets = List.of(createSubnetRequest(CIDR_1), createSubnetRequest(CIDR_2), createSubnetRequest(CIDR_3));

        List<SubnetRequest> actual = underTest.provide(ec2Client, publicSubnets, privateSubnets);

        assertEquals(CIDR_4, actual.get(0).getPublicSubnetCidr());
        assertEquals(AZ_1, actual.get(0).getAvailabilityZone());
        assertEquals(CIDR_5, actual.get(1).getPublicSubnetCidr());
        assertEquals(AZ_2, actual.get(1).getAvailabilityZone());
        assertEquals(CIDR_6, actual.get(2).getPublicSubnetCidr());
        assertEquals(AZ_3, actual.get(2).getAvailabilityZone());

        assertEquals(CIDR_1, actual.get(3).getPrivateSubnetCidr());
        assertEquals(AZ_1, actual.get(3).getAvailabilityZone());
        assertEquals(CIDR_2, actual.get(4).getPrivateSubnetCidr());
        assertEquals(AZ_2, actual.get(4).getAvailabilityZone());
        assertEquals(CIDR_3, actual.get(5).getPrivateSubnetCidr());
        assertEquals(AZ_3, actual.get(5).getAvailabilityZone());
    }

    @Test
    public void testProvideWhenOnlyTwoCidrProvided() {
        AmazonEc2Client ec2Client = createEc2Client(List.of(createAZ(AZ_1), createAZ(AZ_2), createAZ(AZ_3), createAZ(AZ_4)));
        List<NetworkSubnetRequest> publicSubnets = List.of(createSubnetRequest(CIDR_1), createSubnetRequest(CIDR_2));

        List<SubnetRequest> actual = underTest.provide(ec2Client, publicSubnets, new ArrayList<>());

        assertEquals(CIDR_1, actual.get(0).getPublicSubnetCidr());
        assertEquals(AZ_1, actual.get(0).getAvailabilityZone());

        assertEquals(CIDR_2, actual.get(1).getPublicSubnetCidr());
        assertEquals(AZ_2, actual.get(1).getAvailabilityZone());

        assertEquals(2, actual.size());
    }

    private AmazonEc2Client createEc2Client(List<AvailabilityZone> availabilityZones) {
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        DescribeAvailabilityZonesResponse result = DescribeAvailabilityZonesResponse.builder()
                .availabilityZones(availabilityZones)
                .build();
        when(ec2Client.describeAvailabilityZones()).thenReturn(result);
        return ec2Client;
    }

    private AvailabilityZone createAZ(String name) {
        return AvailabilityZone.builder().zoneName(name).build();
    }

    private NetworkSubnetRequest createSubnetRequest(String s) {
        return new NetworkSubnetRequest(s, PUBLIC);
    }
}
