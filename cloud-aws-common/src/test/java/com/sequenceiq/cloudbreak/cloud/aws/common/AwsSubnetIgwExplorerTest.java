package com.sequenceiq.cloudbreak.cloud.aws.common;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.RouteTableAssociation;
import software.amazon.awssdk.services.ec2.model.VpcEndpoint;
import software.amazon.awssdk.services.ec2.model.VpcEndpointType;

@ExtendWith(MockitoExtension.class)
public class AwsSubnetIgwExplorerTest {

    private static final String SUBNET_ID = "subnetId";

    private static final String VPC_ID = "vpcId";

    private static final String DIFFERENT_SUBNET_ID = "differentSubnet";

    private static final String GATEWAY_ID = "igw-4a6s5d4fsadf";

    private static final String FIREWALL_VPCE_ID = "vpce-4fsf45et";

    private static final String NAT_GATEWAY_ID = "nat-4a6s5d4fsadf";

    private static final String VGW_GATEWAY_ID = "vgw-4a6s5d4fsadf";

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String INTERNAL_DESTINATION_CIDR_BLOCK = "172.17.0.0/16";

    private final AwsSubnetIgwExplorer awsSubnetIgwExplorer = new AwsSubnetIgwExplorer();

    @Test
    public void testWithNoRouteTable() {
        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(), SUBNET_ID, VPC_ID, List.of(), false);
        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithValidIgw() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithVirtualPrivateGateway() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(VGW_GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithIgwButNoAssociation() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(DIFFERENT_SUBNET_ID).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithAssociationButNoIgw() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithNoAssociationAndNoIgw() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .associations(RouteTableAssociation.builder().subnetId(DIFFERENT_SUBNET_ID).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithRouteButNoAssociations() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgwPresentWithNotOpenCidrBlockAsDestination() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgwPresentWithNotOpenCidrButVPCEforTrialIsThereBlockAsDestinationAndTrialEntitlement() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(
                        Route.builder()
                                .gatewayId(GATEWAY_ID)
                                .destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK)
                                .build(),
                        Route.builder()
                                .gatewayId("vpce-124")
                                .destinationPrefixListId("pl-1")
                                .destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK)
                                .build(),
                        Route.builder()
                                .gatewayId("vpce-123")
                                .destinationCidrBlock(OPEN_CIDR_BLOCK)
                                .build()
                        )
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), true);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgwPresentWithNotOpenCidrButVPCEforTrialIsThereBlockAsDestinationAndTrialEntitlementNotGranted() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(
                        Route.builder()
                                .gatewayId(GATEWAY_ID)
                                .destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK)
                                .build(),
                        Route.builder()
                                .gatewayId("vpce-124")
                                .destinationPrefixListId("pl-1")
                                .destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK)
                                .build(),
                        Route.builder()
                                .gatewayId("vpce-123")
                                .destinationCidrBlock(OPEN_CIDR_BLOCK)
                                .build()
                )
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndNoIgw() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainRouteTableAssociationAndIgw() {
        RouteTable routeTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(List.of(routeTable), SUBNET_ID, VPC_ID, List.of(), false);

        assertTrue(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndNoIgwAttachedToThem() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();
        routeTables.add(customRouteTable);

        RouteTable custom2RouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();
        routeTables.add(custom2RouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTablesWithAssociationsAndIgwAttachedToMainRouteTable() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();
        routeTables.add(customRouteTable);

        RouteTable custom2RouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();
        routeTables.add(custom2RouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, SUBNET_ID, VPC_ID, List.of(), false);

        assertFalse(hasInternetGateway);
    }

    @Test
    public void testWithMainAndCustomRouteTableWithAssociationsAndIgwAttachedToACustomRouteTable() {
        List<RouteTable> routeTables = new ArrayList<>();
        RouteTable mainRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().destinationCidrBlock(INTERNAL_DESTINATION_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().main(Boolean.TRUE).build())
                .build();
        routeTables.add(mainRouteTable);

        RouteTable customRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(SUBNET_ID).build())
                .build();
        routeTables.add(customRouteTable);

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, SUBNET_ID, VPC_ID, List.of(), false);

        assertTrue(hasInternetGateway);
    }

    @Test
    void testWithProtectedSubnetHavingFirewallVpceRoute() {
        List<RouteTable> routeTables = new ArrayList<>();
        String fireWallVpceId = "vpce-5re43er";
        String firewallSubnetId = "firewallSubnetId";
        RouteTable firewallRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(firewallSubnetId).build())
                .build();
        routeTables.add(firewallRouteTable);

        String protectedPublicSubnetId = "protectedPublicSubnetId";
        RouteTable protectedPublicRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(fireWallVpceId).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(protectedPublicSubnetId).build())
                .build();
        routeTables.add(protectedPublicRouteTable);

        String privateSubnetId = "privateSubnetId";
        RouteTable privateRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().natGatewayId(NAT_GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(privateSubnetId).build())
                .build();
        routeTables.add(privateRouteTable);

        VpcEndpoint firewallVpce = VpcEndpoint.builder()
                .vpcId(VPC_ID)
                .vpcEndpointId(fireWallVpceId)
                .subnetIds(List.of(firewallSubnetId))
                .vpcEndpointType(VpcEndpointType.GATEWAY_LOAD_BALANCER)
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, protectedPublicSubnetId, VPC_ID, List.of(firewallVpce),
                false);

        assertTrue(hasInternetGateway);
    }

    @Test
    void testWithProtectedSubnetWhenSubnetIsNotPresentInFirewallVpce() {
        List<RouteTable> routeTables = new ArrayList<>();
        String fireWallVpceId = "vpce-5re43er";
        String firewallSubnetId = "firewallSubnetId";
        RouteTable firewallRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(firewallSubnetId).build())
                .build();
        routeTables.add(firewallRouteTable);

        String protectedPublicSubnetId = "protectedPublicSubnetId";
        RouteTable protectedPublicRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(fireWallVpceId).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(protectedPublicSubnetId).build())
                .build();
        routeTables.add(protectedPublicRouteTable);

        String privateSubnetId = "privateSubnetId";
        RouteTable privateRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().natGatewayId(NAT_GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(privateSubnetId).build())
                .build();
        routeTables.add(privateRouteTable);

        VpcEndpoint firewallVpce = VpcEndpoint.builder()
                .vpcId(VPC_ID)
                .vpcEndpointId(fireWallVpceId)
                .subnetIds(List.of())
                .vpcEndpointType(VpcEndpointType.GATEWAY_LOAD_BALANCER)
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, protectedPublicSubnetId, VPC_ID, List.of(firewallVpce),
                false);

        assertFalse(hasInternetGateway);
    }

    @Test
    void testWithProtectedSubnetWhenNoIgwRouteInFirewallRouteTable() {
        List<RouteTable> routeTables = new ArrayList<>();
        String fireWallVpceId = "vpce-5re43er";
        String firewallSubnetId = "firewallSubnetId";
        RouteTable firewallRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().natGatewayId(NAT_GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(firewallSubnetId).build())
                .build();
        routeTables.add(firewallRouteTable);

        String protectedPublicSubnetId = "protectedPublicSubnetId";
        RouteTable protectedPublicRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().gatewayId(fireWallVpceId).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(protectedPublicSubnetId).build())
                .build();
        routeTables.add(protectedPublicRouteTable);

        String privateSubnetId = "privateSubnetId";
        RouteTable privateRouteTable = RouteTable.builder()
                .vpcId(VPC_ID)
                .routes(Route.builder().natGatewayId(NAT_GATEWAY_ID).destinationCidrBlock(OPEN_CIDR_BLOCK).build())
                .associations(RouteTableAssociation.builder().subnetId(privateSubnetId).build())
                .build();
        routeTables.add(privateRouteTable);

        VpcEndpoint firewallVpce = VpcEndpoint.builder()
                .vpcId(VPC_ID)
                .vpcEndpointId(fireWallVpceId)
                .subnetIds(List.of(firewallSubnetId))
                .vpcEndpointType(VpcEndpointType.GATEWAY_LOAD_BALANCER)
                .build();

        boolean hasInternetGateway = awsSubnetIgwExplorer.hasInternetGatewayOrVpceOfSubnet(routeTables, protectedPublicSubnetId, VPC_ID, List.of(firewallVpce),
                false);

        assertFalse(hasInternetGateway);
    }
}
