package com.sequenceiq.cloudbreak.cloud.aws;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;

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
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);
        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithValidIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithVirtualPrivateGateway() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(VGW_GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithIgwButNoAssociation() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithAssociationButNoIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithNoAssociationAndNoIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithRouteButNoAssociations() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgwPresentWithNotOpenCidrBlockAsDestination() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndNoIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withDestinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable().withVpcId(VPC_ID);
        Route route = new Route()
                .withGatewayId(GATEWAY_ID)
                .withDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation()
                .withMain(Boolean.TRUE);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndNoIgwAttachedToThem() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        Set<RouteTable> routeTables = new HashSet<>();
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
        describeRouteTablesResult.setRouteTables(routeTables);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndIgwAttachedToMainRouteTable() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        Set<RouteTable> routeTables = new HashSet<>();
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
        describeRouteTablesResult.setRouteTables(routeTables);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTableWithAssociationsAndIgwAttachedToACustomRouteTable() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        Set<RouteTable> routeTables = new HashSet<>();
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
        describeRouteTablesResult.setRouteTables(routeTables);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID, VPC_ID);

        assertTrue(hasInternetGateway);
    }
}