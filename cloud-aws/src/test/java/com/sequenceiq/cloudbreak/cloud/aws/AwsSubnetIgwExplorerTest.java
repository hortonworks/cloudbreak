package com.sequenceiq.cloudbreak.cloud.aws;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;

public class AwsSubnetIgwExplorerTest {

    private static final String SUBNET_ID = "subnetId";

    private static final String DIFFERENT_SUBNET_ID = "differentSubnet";

    private static final String GATEWAY_ID = "igw-4a6s5d4fsadf";

    private static final String VGW_GATEWAY_ID = "vgw-4a6s5d4fsadf";

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private final AwsSubnetIgwExplorer awsSubnetIgwExplorer = new AwsSubnetIgwExplorer();

    @Test
    public void testWithNoRouteTable() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);
        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithValidIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithVirtualPrivateGateway() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        route.setGatewayId(VGW_GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithIgwButNoAssociation() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        route.setGatewayId(GATEWAY_ID);
        route.setDestinationCidrBlock(OPEN_CIDR_BLOCK);
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithAssociationButNoIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithNoAssociationAndNoIgw() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        RouteTableAssociation routeTableAssociation = new RouteTableAssociation();
        routeTableAssociation.setSubnetId(DIFFERENT_SUBNET_ID);
        routeTable.setAssociations(List.of(routeTableAssociation));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithRouteButNoAssociations() {
        DescribeRouteTablesResult describeRouteTablesResult = new DescribeRouteTablesResult();
        RouteTable routeTable = new RouteTable();
        Route route = new Route();
        routeTable.setRoutes(List.of(route));
        describeRouteTablesResult.setRouteTables(List.of(routeTable));

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(describeRouteTablesResult, SUBNET_ID);

        assertFalse(hasInternetGateway);
    }

}