package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Subnet;

@ExtendWith(MockitoExtension.class)
class AwsGatewaySubnetMultiAzValidatorTest {

    @Mock
    private CommonAwsClient awsClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonEc2Client ec2Client;

    @InjectMocks
    private AwsGatewaySubnetMultiAzValidator underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext().getLocation().getRegion().value()).thenReturn("aRegion");
    }

    @Test
    void testValidateWhenThereIsNoGroup() {
        CloudStack cloudStack = getCloudStack(Set.of());

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testValidateWhenThereIsNoGatewayGroup() {
        Group coreGroup = getGroup("aGroupName", InstanceGroupType.CORE, null);
        CloudStack cloudStack = getCloudStack(Set.of(coreGroup));

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testValidateWhenThereIsGatewayGroupButNoInstanceGroupNetwork() {
        Group gatewayGroup = getGroup("aGroupName", InstanceGroupType.GATEWAY, null);
        CloudStack cloudStack = getCloudStack(Set.of(gatewayGroup));

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testValidateWhenThereIsGatewayGroupButInstanceGroupNetworkDoesNotHaveSubnetsConfigured() {
        GroupNetwork groupNetwork = new GroupNetwork(OutboundInternetTraffic.ENABLED, null, Map.of());
        Group gatewayGroup = getGroup("aGroupName", InstanceGroupType.GATEWAY, groupNetwork);
        CloudStack cloudStack = getCloudStack(Set.of(gatewayGroup));

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testValidateWhenThereIsGatewayGroupButInstanceGroupNetworkHasValidSubnetConfiguration() {
        String aSubnetId = "aSubnetId";
        Set<GroupSubnet> of = Set.of(new GroupSubnet(aSubnetId));
        GroupNetwork groupNetwork = new GroupNetwork(OutboundInternetTraffic.ENABLED, of, Map.of());
        Group gatewayGroup = getGroup("aGroupName", InstanceGroupType.GATEWAY, groupNetwork);
        CloudStack cloudStack = getCloudStack(Set.of(gatewayGroup));
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        DescribeSubnetsResponse describeSubnetsResponse = DescribeSubnetsResponse.builder()
                .subnets(Set.of(Subnet.builder().subnetId(aSubnetId).availabilityZone("anAZ").build()))
                .build();
        when(ec2Client.describeSubnets(any())).thenReturn(describeSubnetsResponse);

        underTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testValidateWhenThereIsGatewayGroupButInstanceGroupNetworkHasConfigurationWithMultipleSubnetPerAvailabilityZone() {
        String aSubnetId = "aSubnetId";
        String availabilityZone = "anAZ";
        Set<GroupSubnet> of = Set.of(new GroupSubnet(aSubnetId));
        GroupNetwork groupNetwork = new GroupNetwork(OutboundInternetTraffic.ENABLED, of, Map.of());
        Group gatewayGroup = getGroup("aGroupName", InstanceGroupType.GATEWAY, groupNetwork);
        CloudStack cloudStack = getCloudStack(Set.of(gatewayGroup));
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        Set<Subnet> subnets = Set.of(Subnet.builder().subnetId(aSubnetId).availabilityZone(availabilityZone).build(),
                Subnet.builder().subnetId("anotherSubnetId").availabilityZone(availabilityZone).build());
        DescribeSubnetsResponse describeSubnetsResponse = DescribeSubnetsResponse.builder()
                .subnets(subnets)
                .build();
        when(ec2Client.describeSubnets(any())).thenReturn(describeSubnetsResponse);

        assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));
    }

    @Test
    void testValidateWhenThereIsGatewayGroupButInstanceGroupNetworkHasValidSubnetAndEC2ClientCallFails() {
        String aSubnetId = "aSubnetId";
        Set<GroupSubnet> of = Set.of(new GroupSubnet(aSubnetId));
        GroupNetwork groupNetwork = new GroupNetwork(OutboundInternetTraffic.ENABLED, of, Map.of());
        Group gatewayGroup = getGroup("aGroupName", InstanceGroupType.GATEWAY, groupNetwork);
        CloudStack cloudStack = getCloudStack(Set.of(gatewayGroup));
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeSubnets(any())).thenThrow(AwsServiceException.builder().message("Something went wrong").build());

        assertThrows(CloudConnectorException.class, () -> underTest.validate(authenticatedContext, cloudStack));
    }

    private CloudStack getCloudStack(Collection<Group> groups) {
        return CloudStack.builder()
                .groups(groups)
                .build();
    }

    private Group getGroup(String groupName, InstanceGroupType groupType, GroupNetwork groupNetwork) {
        return Group.builder()
                .withName(groupName)
                .withType(groupType)
                .withNetwork(groupNetwork)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build();
    }
}
