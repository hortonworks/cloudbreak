package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsNativeLoadBalancerService.CROSS_ZONE_LOAD_BALANCING_ENABLED;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancerAttributesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ElasticLoadBalancingV2Exception;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SetSubnetsRequest;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerServiceTest {

    private static final String REGION_NAME = "regionName";

    private static final String AZ = "AZ";

    private static final long STACK_ID = 1L;

    private static final String LB_ARN = "arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/my-lb/50dc6c495c0c9188";

    private static final String LB_NAME = "my-lb";

    private static final String LB_PRIVATE_IP_1 = "1.1.1.1";

    private static final String LB_PRIVATE_IP_2 = "2.2.2.2";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AmazonElasticLoadBalancingClient amazonElbClient;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private AwsNativeLoadBalancerService underTest;

    @Test
    void testUpdateMultiAzLoadBalancersWhenNoLoadBalancersExistReturnsEmpty() {
        setUpAwsClient();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID))
                .thenReturn(emptyList());

        underTest.updateMultiAzLoadBalancers(ac, emptyStack());

        verifyNoInteractions(amazonElbClient);
    }

    @Test
    void testUpdateMultiAzLoadBalancersEnablesCrossZoneWhenDisabled() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(false);
        stubCurrentSubnets("subnet-1");

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1"));

        ArgumentCaptor<ModifyLoadBalancerAttributesRequest> captor = ArgumentCaptor.forClass(ModifyLoadBalancerAttributesRequest.class);
        verify(amazonElbClient).modifyLoadBalancerAttributes(captor.capture());
        ModifyLoadBalancerAttributesRequest request = captor.getValue();
        assertThat(request.loadBalancerArn()).isEqualTo(LB_ARN);
        assertThat(request.attributes()).singleElement().satisfies(attr -> {
            assertThat(attr.key()).isEqualTo(CROSS_ZONE_LOAD_BALANCING_ENABLED);
            assertThat(attr.value()).isEqualTo("true");
        });
    }

    @Test
    void testUpdateMultiAzLoadBalancersSkipsCrossZoneModifyWhenAlreadyEnabled() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1"));

        verify(amazonElbClient, never()).modifyLoadBalancerAttributes(any());
    }

    @Test
    void testUpdateMultiAzLoadBalancersAddsMissingSubnets() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");
        stubSubnetZones(Map.of("subnet-2", zoneOf("subnet-2")));

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2"));

        ArgumentCaptor<SetSubnetsRequest> captor = ArgumentCaptor.forClass(SetSubnetsRequest.class);
        verify(amazonElbClient).setSubnets(captor.capture());
        SetSubnetsRequest request = captor.getValue();
        assertThat(request.loadBalancerArn()).isEqualTo(LB_ARN);
        assertThat(request.subnets()).containsExactlyInAnyOrder("subnet-1", "subnet-2");
    }

    @Test
    void testUpdateMultiAzLoadBalancersAddsOnlyOneSubnetPerAvailabilityZone() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");
        stubSubnetZones(Map.of("subnet-2", "az-shared", "subnet-3", "az-shared"));

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2", "subnet-3"));

        ArgumentCaptor<SetSubnetsRequest> captor = ArgumentCaptor.forClass(SetSubnetsRequest.class);
        verify(amazonElbClient).setSubnets(captor.capture());
        assertThat(captor.getValue().subnets()).containsExactlyInAnyOrder("subnet-1", "subnet-2");
    }

    @Test
    void testUpdateMultiAzLoadBalancersSkipsSubnetInAlreadyServedAvailabilityZone() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");
        stubSubnetZones(Map.of("subnet-2", zoneOf("subnet-1")));

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2"));

        verify(amazonElbClient, never()).setSubnets(any(SetSubnetsRequest.class));
    }

    @Test
    void testUpdateMultiAzLoadBalancersSkipsSubnetWithUnknownAvailabilityZone() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");
        stubSubnetZones(Map.of());

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2"));

        verify(amazonElbClient, never()).setSubnets(any(SetSubnetsRequest.class));
    }

    @Test
    void testUpdateMultiAzLoadBalancersSkipsSetSubnetsWhenAlreadyUpToDate() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1", "subnet-2");

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2"));

        verify(amazonElbClient, never()).setSubnets(any(SetSubnetsRequest.class));
    }

    @Test
    void testUpdateMultiAzLoadBalancersSkipsSetSubnetsWhenStackHasNoSubnets() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);

        underTest.updateMultiAzLoadBalancers(ac, emptyStack());

        verify(amazonElbClient, never()).describeLoadBalancers(any(DescribeLoadBalancersRequest.class));
        verify(amazonElbClient, never()).setSubnets(any(SetSubnetsRequest.class));
    }

    @Test
    void testUpdateMultiAzLoadBalancersQueriesAttributesForCorrectLoadBalancer() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCrossZoneEnabled(true);
        stubCurrentSubnets("subnet-1");

        underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1"));

        ArgumentCaptor<DescribeLoadBalancerAttributesRequest> captor = ArgumentCaptor.forClass(DescribeLoadBalancerAttributesRequest.class);
        verify(amazonElbClient).describeLoadBalancerAttributes(captor.capture());
        assertThat(captor.getValue().loadBalancerArn()).isEqualTo(LB_ARN);
    }

    @Test
    void testUpdateMultiAzLoadBalancersWrapsAwsExceptionInCloudConnectorException() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        ElasticLoadBalancingV2Exception awsException = (ElasticLoadBalancingV2Exception)
                ElasticLoadBalancingV2Exception.builder().message("boom").build();
        when(amazonElbClient.describeLoadBalancerAttributes(any(DescribeLoadBalancerAttributesRequest.class)))
                .thenThrow(awsException);

        assertThatThrownBy(() -> underTest.updateMultiAzLoadBalancers(ac, stackWithSubnets("subnet-1")))
                .isInstanceOf(CloudConnectorException.class)
                .hasCauseReference(awsException);
    }

    @Test
    void testWaitForLoadBalancersSkipsWhenStackHasNoSubnets() {
        setUpAwsClient();

        underTest.waitForLoadBalancers(ac, emptyStack());

        verifyNoInteractions(resourceRetriever, amazonElbClient, amazonEc2Client);
    }

    @Test
    void testWaitForLoadBalancersReturnsWhenEnisAreOnAllExpectedSubnets() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCurrentSubnets("subnet-1");
        stubEniSubnets("subnet-1");

        underTest.waitForLoadBalancers(ac, stackWithSubnets("subnet-1"));

        ArgumentCaptor<DescribeNetworkInterfacesRequest> captor = ArgumentCaptor.forClass(DescribeNetworkInterfacesRequest.class);
        verify(amazonEc2Client).describeNetworkInterfaces(captor.capture());
        assertThat(captor.getValue().filters()).singleElement().satisfies(filter -> {
            assertThat(filter.name()).isEqualTo("description");
            assertThat(filter.values()).containsExactly("ELB net/" + LB_NAME + "/*");
        });
    }

    @Test
    void testWaitForLoadBalancersWaitsForNewlyAddedSubnetToo() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCurrentSubnets("subnet-1");
        stubSubnetZones(Map.of("subnet-2", zoneOf("subnet-2")));
        stubEniSubnets("subnet-1", "subnet-2");

        underTest.waitForLoadBalancers(ac, stackWithSubnets("subnet-1", "subnet-2"));

        verify(amazonEc2Client).describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class));
    }

    @Test
    void testWaitForLoadBalancersIgnoresNetworkInterfacesWithoutSubnet() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCurrentSubnets("subnet-1");
        when(amazonEc2Client.describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class)))
                .thenReturn(DescribeNetworkInterfacesResponse.builder()
                        .networkInterfaces(NetworkInterface.builder().build(), NetworkInterface.builder().subnetId("subnet-1").build())
                        .build());

        underTest.waitForLoadBalancers(ac, stackWithSubnets("subnet-1"));

        verify(amazonEc2Client).describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class));
    }

    @Test
    void testWaitForLoadBalancersThrowsCloudConnectorExceptionWhenDescribeEnisFails() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        stubCurrentSubnets("subnet-1");
        when(amazonEc2Client.describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class)))
                .thenThrow(Ec2Exception.builder().message("boom").build());

        CloudStack stack = stackWithSubnets("subnet-1");
        assertThatThrownBy(() -> underTest.waitForLoadBalancers(ac, stack))
                .isInstanceOf(CloudConnectorException.class);
    }

    @Test
    void testWaitForLoadBalancersThrowsWhenLoadBalancerIsNotFound() {
        setUpAwsClient();
        setUpLoadBalancerResource();
        when(amazonElbClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class)))
                .thenReturn(DescribeLoadBalancersResponse.builder().loadBalancers(emptyList()).build());

        CloudStack stack = stackWithSubnets("subnet-1");
        assertThatThrownBy(() -> underTest.waitForLoadBalancers(ac, stack))
                .isInstanceOf(CloudConnectorException.class)
                .hasMessageContaining(LB_ARN);
        verify(amazonEc2Client, never()).describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class));
    }

    @Test
    void testGetLoadBalancerIpShouldReturnLoadBalancerIpAddresses() {
        when(amazonEc2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(NetworkInterface.builder().privateIpAddress(LB_PRIVATE_IP_1).build(),
                        NetworkInterface.builder().privateIpAddress(LB_PRIVATE_IP_2).build())
                .build());

        List<String> actual = underTest.getLoadBalancerIps(amazonEc2Client, LB_NAME);

        assertThat(actual).containsExactly(LB_PRIVATE_IP_1, LB_PRIVATE_IP_2);
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponseIsNull() {
        when(amazonEc2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder().build());

        assertThrows(NotFoundException.class, () -> underTest.getLoadBalancerIps(amazonEc2Client, LB_NAME));
    }

    @Test
    void testGetLoadBalancerIpShouldThrowExceptionWhenTheNetworkInterfaceResponsePrivateIpIsNull() {
        when(amazonEc2Client.describeNetworkInterfaces(any())).thenReturn(DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(NetworkInterface.builder().build(), NetworkInterface.builder().build())
                .build());

        assertThrows(NotFoundException.class, () -> underTest.getLoadBalancerIps(amazonEc2Client, LB_NAME));
    }

    @Test
    void testGetLoadBalancerIpShouldThrowCloudConnectorExceptionWhenThereIsNoPermission() {
        doThrow(Ec2Exception.builder().message("No permission").build()).when(amazonEc2Client).describeNetworkInterfaces(any());

        assertThrows(CloudConnectorException.class, () -> underTest.getLoadBalancerIps(amazonEc2Client, LB_NAME));
    }

    private void stubEniSubnets(String... subnetIds) {
        DescribeNetworkInterfacesResponse response = DescribeNetworkInterfacesResponse.builder()
                .networkInterfaces(Arrays.stream(subnetIds)
                        .map(subnetId -> NetworkInterface.builder().subnetId(subnetId).build())
                        .toList())
                .build();
        when(amazonEc2Client.describeNetworkInterfaces(any(DescribeNetworkInterfacesRequest.class))).thenReturn(response);
    }

    private void setUpAwsClient() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME), AvailabilityZone.availabilityZone(AZ)));
        when(awsClient.createElasticLoadBalancingClient(any(), any())).thenReturn(amazonElbClient);
        lenient().when(awsClient.createEc2Client(any(), any())).thenReturn(amazonEc2Client);
    }

    private void setUpLoadBalancerResource() {
        CloudResource lbResource = CloudResource.builder()
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .withStatus(CommonStatus.CREATED)
                .withName("lb-name")
                .withReference(LB_ARN)
                .withParameters(Collections.emptyMap())
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID))
                .thenReturn(List.of(lbResource));
    }

    private void stubCrossZoneEnabled(boolean enabled) {
        DescribeLoadBalancerAttributesResponse response = DescribeLoadBalancerAttributesResponse.builder()
                .attributes(LoadBalancerAttribute.builder()
                        .key(CROSS_ZONE_LOAD_BALANCING_ENABLED)
                        .value(Boolean.toString(enabled))
                        .build())
                .build();
        when(amazonElbClient.describeLoadBalancerAttributes(any(DescribeLoadBalancerAttributesRequest.class))).thenReturn(response);
    }

    private void stubCurrentSubnets(String... subnetIds) {
        software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone[] zones =
                new software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone[subnetIds.length];
        for (int i = 0; i < subnetIds.length; i++) {
            zones[i] = software.amazon.awssdk.services.elasticloadbalancingv2.model.AvailabilityZone.builder()
                    .subnetId(subnetIds[i])
                    .zoneName(zoneOf(subnetIds[i]))
                    .build();
        }
        DescribeLoadBalancersResponse response = DescribeLoadBalancersResponse.builder()
                .loadBalancers(LoadBalancer.builder().loadBalancerName(LB_NAME).availabilityZones(zones).build())
                .build();
        when(amazonElbClient.describeLoadBalancers(any(DescribeLoadBalancersRequest.class))).thenReturn(response);
    }

    private static String zoneOf(String subnetId) {
        return "az-" + subnetId;
    }

    private void stubSubnetZones(Map<String, String> zoneBySubnetId) {
        DescribeSubnetsResponse response = DescribeSubnetsResponse.builder()
                .subnets(zoneBySubnetId.entrySet().stream()
                        .map(entry -> Subnet.builder().subnetId(entry.getKey()).availabilityZone(entry.getValue()).build())
                        .toList())
                .build();
        when(amazonEc2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(response);
    }

    private CloudStack emptyStack() {
        return CloudStack.builder().groups(emptyList()).build();
    }

    private CloudStack stackWithSubnets(String... subnetIds) {
        Set<GroupSubnet> subnets = Arrays.stream(subnetIds).map(GroupSubnet::new).collect(Collectors.toSet());
        GroupNetwork network = new GroupNetwork(OutboundInternetTraffic.DISABLED, subnets, Map.of());
        Group group = Group.builder().withNetwork(network).build();
        return CloudStack.builder().groups(List.of(group)).build();
    }
}
