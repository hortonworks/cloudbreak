package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer.ENDPOINT_GATEWAY_OVERRIDE_KEY;
import static com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer.ENDPOINT_GATEWAY_OVERRIDE_VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mock;

import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.networkfirewall.model.Attachment;
import com.amazonaws.services.networkfirewall.model.DescribeFirewallResult;
import com.amazonaws.services.networkfirewall.model.Firewall;
import com.amazonaws.services.networkfirewall.model.FirewallMetadata;
import com.amazonaws.services.networkfirewall.model.FirewallStatus;
import com.amazonaws.services.networkfirewall.model.ListFirewallsResult;
import com.amazonaws.services.networkfirewall.model.SyncState;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonNetworkFirewallClient;

public class AwsSubnetIgwExplorerTest {

    private static final String SUBNET_ID = "subnetId";

    private static final String VPC_ID = "vpcId";

    private static final String DIFFERENT_SUBNET_ID = "differentSubnet";

    private static final String GATEWAY_ID = "igw-4a6s5d4fsadf";

    private static final String VGW_GATEWAY_ID = "vgw-4a6s5d4fsadf";

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String INTERNAL_DESTINATION_CIDR_BLOCK = "172.17.0.0/16";

    private static final String FW_SUBNET_ID = "fwSubnetId";

    private static final String FW_GATEWAY_ID = "vpce-4a6s5d4fsadf";

    private static final String FW_ARN = "arn:firewall";

    @Mock
    private final AmazonNetworkFirewallClient nfwClient = mock(AmazonNetworkFirewallClient.class);

    private final AwsSubnetIgwExplorer awsSubnetIgwExplorer = new AwsSubnetIgwExplorer();

    @Test
    public void testWithNoRouteTable() {
        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(), SUBNET_ID, VPC_ID);
        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithValidIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithVirtualPrivateGateway() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(VGW_GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithIgwButNoAssociation() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithAssociationButNoIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithNoAssociationAndNoIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithRouteButNoAssociations() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgwPresentWithNotOpenCidrBlockAsDestination() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndNoIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndNoIgwAttachedToThem() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route mainRoute = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        mainRouteTable.setRoutes(List.of(mainRoute));
        RouteTableAssociation mainRouteTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        mainRouteTable.setAssociations(List.of(mainRouteTableAssociation));
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route customRoute = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        customRouteTable.setRoutes(List.of(customRoute));
        RouteTableAssociation customRouteTableAssociation = new RouteTableAssociation()
                .withSubnetId(SUBNET_ID);
        customRouteTable.setAssociations(List.of(customRouteTableAssociation));
        routeTables.add(customRouteTable);

        RouteTable custom2RouteTable = new RouteTable().withVpcId(VPC_ID);
        Route custom2Route = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        custom2RouteTable.setRoutes(List.of(custom2Route));
        RouteTableAssociation custom2RouteTableAssociation = new RouteTableAssociation()
                .withSubnetId(SUBNET_ID);
        custom2RouteTable.setAssociations(List.of(custom2RouteTableAssociation));
        routeTables.add(custom2RouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(routeTables, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndIgwAttachedToMainRouteTable() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route mainRoute = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(OPEN_CIDR_BLOCK);
        mainRouteTable.setRoutes(List.of(mainRoute));
        RouteTableAssociation mainRouteTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        mainRouteTable.setAssociations(List.of(mainRouteTableAssociation));
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route customRoute = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        customRouteTable.setRoutes(List.of(customRoute));
        RouteTableAssociation customRouteTableAssociation = new RouteTableAssociation()
                .withSubnetId(SUBNET_ID);
        customRouteTable.setAssociations(List.of(customRouteTableAssociation));
        routeTables.add(customRouteTable);

        RouteTable custom2RouteTable = new RouteTable().withVpcId(VPC_ID);
        Route custom2Route = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        custom2RouteTable.setRoutes(List.of(custom2Route));
        RouteTableAssociation custom2RouteTableAssociation = new RouteTableAssociation()
                .withSubnetId(SUBNET_ID);
        custom2RouteTable.setAssociations(List.of(custom2RouteTableAssociation));
        routeTables.add(custom2RouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(routeTables, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTableWithAssociationsAndIgwAttachedToACustomRouteTable() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route mainRoute = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        mainRouteTable.setRoutes(List.of(mainRoute));
        RouteTableAssociation mainRouteTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        mainRouteTable.setAssociations(List.of(mainRouteTableAssociation));
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = new RouteTable().withVpcId(VPC_ID);
        Route custom2Route = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(OPEN_CIDR_BLOCK);
        customRouteTable.setRoutes(List.of(custom2Route));
        RouteTableAssociation custom2RouteTableAssociation = new RouteTableAssociation()
                .withSubnetId(SUBNET_ID);
        customRouteTable.setAssociations(List.of(custom2RouteTableAssociation));
        routeTables.add(customRouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(routeTables, SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testInternetRoutableWithIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        setupFirewallClientMocks();

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, List.of(routeTable),
            createSubnets(null, null), SUBNET_ID, VPC_ID);

        assertTrue(routableToInternet);
    }

    @Test
    public void testInternetRoutableWithoutIgw() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        setupFirewallClientMocks();

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, List.of(routeTable),
            createSubnets(null, null), SUBNET_ID, VPC_ID);

        assertFalse(routableToInternet);
    }

    @Test
    public void testFirewallRoutingWithIgw() {
        setupFirewallClientMocks();
        List<RouteTable> routeTables = setupFirewallRoutes(true);

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, routeTables,
            createSubnets(null, null), SUBNET_ID, VPC_ID);

        assertTrue(routableToInternet);
    }

    @Test
    public void testFirewallRoutingWithoutIgw() {
        setupFirewallClientMocks();
        List<RouteTable> routeTables = setupFirewallRoutes(false);

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, routeTables,
            createSubnets(null, null), SUBNET_ID, VPC_ID);

        assertFalse(routableToInternet);
    }

    @Test
    public void testEndpointGatewayTagOverride() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        List<Subnet> subnets = createSubnets(ENDPOINT_GATEWAY_OVERRIDE_KEY, ENDPOINT_GATEWAY_OVERRIDE_VALUE);

        setupFirewallClientMocks();

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, List.of(routeTable),
            subnets, SUBNET_ID, VPC_ID);

        assertTrue(routableToInternet);
    }

    @Test
    public void testEndpointGatewayTagOverrideBadValue() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        List<Subnet> subnets = createSubnets(ENDPOINT_GATEWAY_OVERRIDE_KEY, "something else");
        setupFirewallClientMocks();

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, List.of(routeTable),
            subnets, SUBNET_ID, VPC_ID);

        assertFalse(routableToInternet);
    }

    @Test
    public void testEndpointGatewayTagNonOverrideTag() {
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));

        List<Subnet> subnets = createSubnets("another tag", ENDPOINT_GATEWAY_OVERRIDE_VALUE);
        setupFirewallClientMocks();

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(nfwClient, List.of(routeTable),
            subnets, SUBNET_ID, VPC_ID);

        assertFalse(routableToInternet);
    }

    private List<RouteTable> setupFirewallRoutes(boolean withIgw) {
        Route subnetRoute = new Route();
        subnetRoute.setGatewayId(FW_GATEWAY_ID);
        subnetRoute.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        RouteTableAssociation subnetRouteTableAssociation = new RouteTableAssociation();
        subnetRouteTableAssociation.setSubnetId(SUBNET_ID);

        Route fwRoute = new Route();
        if (withIgw) {
            fwRoute.setGatewayId(GATEWAY_ID);
        }
        fwRoute.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        RouteTableAssociation fwRouteTableAssociation = new RouteTableAssociation();
        fwRouteTableAssociation.setSubnetId(FW_SUBNET_ID);

        RouteTable subnetRouteTable = new RouteTable().withVpcId(VPC_ID);
        subnetRouteTable.setAssociations(List.of(subnetRouteTableAssociation));
        subnetRouteTable.setRoutes(List.of(subnetRoute));
        subnetRouteTable.setVpcId(VPC_ID);

        RouteTable fwRouteTable = new RouteTable().withVpcId(VPC_ID);
        fwRouteTable.setAssociations(List.of(fwRouteTableAssociation));
        fwRouteTable.setRoutes(List.of(fwRoute));
        fwRouteTable.setVpcId(VPC_ID);

        return List.of(subnetRouteTable, fwRouteTable);
    }

    private void setupFirewallClientMocks() {
        FirewallMetadata firewallMetadata = new FirewallMetadata();
        firewallMetadata.setFirewallArn(FW_ARN);

        ListFirewallsResult listFirewallsResult = new ListFirewallsResult();
        listFirewallsResult.setFirewalls(List.of(firewallMetadata));

        Attachment attachment = new Attachment();
        attachment.setEndpointId(FW_GATEWAY_ID);
        attachment.setSubnetId(FW_SUBNET_ID);

        SyncState syncState = new SyncState();
        syncState.setAttachment(attachment);

        FirewallStatus firewallStatus = new FirewallStatus();
        firewallStatus.setSyncStates(Map.of("key", syncState));

        Firewall firewall = new Firewall();
        firewall.setFirewallName("name");

        DescribeFirewallResult describeFirewallResult = new DescribeFirewallResult();
        describeFirewallResult.setFirewallStatus(firewallStatus);
        describeFirewallResult.setFirewall(firewall);

        when(nfwClient.listFirewalls(any())).thenReturn(listFirewallsResult);
        when(nfwClient.describeFirewall(any())).thenReturn(describeFirewallResult);
    }

    private List<Subnet> createSubnets(String tagKey, String tagValue) {
        Subnet subnet = new Subnet();
        subnet.setSubnetId(SUBNET_ID);
        subnet.setVpcId(VPC_ID);

        if (StringUtils.isNotEmpty(tagKey) && StringUtils.isNotEmpty(tagValue)) {
            Tag tag = new Tag();
            tag.setKey(tagKey);
            tag.setValue(tagValue);
            subnet.setTags(List.of(tag));
        }

        return List.of(subnet);
    }
}