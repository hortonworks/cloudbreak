package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;

@Component
public class AwsSubnetIgwExplorer {

    static final String ENDPOINT_GATEWAY_OVERRIDE_KEY = "cdp-public-endpoint-gateway-lb";

    static final String ENDPOINT_GATEWAY_OVERRIDE_VALUE = "override";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSubnetIgwExplorer.class);

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String IGW_PREFIX = "igw-";

    public boolean hasInternetGatewayOfSubnet(List<RouteTable> routeTables, String subnetId, String vpcId) {
        Optional<RouteTable> routeTable = getRouteTableForSubnet(routeTables, subnetId, vpcId);
        LOGGER.debug("Route table for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeTable);

        Optional<Route> routeWithInternetGateway = getRouteWithInternetGateway(routeTable);
        LOGGER.debug("Route with IGW for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeWithInternetGateway);

        return routeWithInternetGateway.isPresent();
    }

    private Optional<RouteTable> getRouteTableForSubnet(List<RouteTable> tableList, String subnetId, String vpcId) {
        List<RouteTable> routeTables = tableList.stream()
                .filter(rt -> rt.vpcId().equalsIgnoreCase(vpcId))
                .collect(Collectors.toList());
        LOGGER.debug("Route tables for VPC '{}' (current subnet is '{}'): '{}'", vpcId, subnetId, routeTables);

        Optional<RouteTable> explicitRouteTable = getExplicitRouteTable(routeTables, subnetId);
        LOGGER.debug("Explicit route table for subnet '{}' (VPC is '{}'): {}", subnetId, vpcId, explicitRouteTable);

        return explicitRouteTable.or(() -> {
            LOGGER.debug("There is no explicit route table for subnet '{}' (VPC is '{}') so looking for the implicit one.", subnetId, vpcId);
            Optional<RouteTable> implicitRouteTable = getImplicitRouteTable(routeTables);
            LOGGER.debug("Implicit route table for subnet '{}' (VPC is '{}'): {}", subnetId, vpcId, implicitRouteTable);
            return implicitRouteTable;
        });
    }

    private Optional<RouteTable> getExplicitRouteTable(List<RouteTable> routeTables, String subnetId) {
        return routeTables.stream()
                .filter(rt -> rt.associations().stream().anyMatch(rta -> subnetId.equalsIgnoreCase(rta.subnetId())))
                .findFirst();
    }

    private Optional<RouteTable> getImplicitRouteTable(List<RouteTable> routeTables) {
        return routeTables.stream()
                .filter(rt -> rt.associations().stream().anyMatch(rta -> StringUtils.isEmpty(rta.subnetId()) && rta.main()))
                .findFirst();
    }

    private Optional<Route> getRouteWithInternetGateway(Optional<RouteTable> routeTable) {
        return routeTable.stream()
                .flatMap(rt -> rt.routes().stream())
                .filter(route -> StringUtils.isNotEmpty(route.gatewayId()) &&
                        route.gatewayId().startsWith(IGW_PREFIX) &&
                        OPEN_CIDR_BLOCK.equals(route.destinationCidrBlock()))
                .findFirst();
    }
}
