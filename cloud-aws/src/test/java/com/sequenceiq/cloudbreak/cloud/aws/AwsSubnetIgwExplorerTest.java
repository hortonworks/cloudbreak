package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer.ENDPOINT_GATEWAY_OVERRIDE_KEY;
import static com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer.ENDPOINT_GATEWAY_OVERRIDE_VALUE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;

public class AwsSubnetIgwExplorerTest {

    private static final String SUBNET_ID = "subnetId";

    private static final String VPC_ID = "vpcId";

    private static final String DIFFERENT_SUBNET_ID = "differentSubnet";

    private static final String GATEWAY_ID = "igw-4a6s5d4fsadf";

    private static final String VGW_GATEWAY_ID = "vgw-4a6s5d4fsadf";

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String INTERNAL_DESTINATION_CIDR_BLOCK = "172.17.0.0/16";

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

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(List.of(routeTable),
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

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(List.of(routeTable),
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

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(List.of(routeTable),
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

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(List.of(routeTable),
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

        boolean routableToInternet = awsSubnetIgwExplorer.isRoutableToInternet(List.of(routeTable),
            subnets, SUBNET_ID, VPC_ID);

        assertFalse(routableToInternet);
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