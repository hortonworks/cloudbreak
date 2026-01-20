package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.VpcEndpoint;
import software.amazon.awssdk.services.ec2.model.VpcEndpointType;

@Component
public class AwsSubnetIgwExplorer {

    static final String ENDPOINT_GATEWAY_OVERRIDE_KEY = "cdp-public-endpoint-gateway-lb";

    static final String ENDPOINT_GATEWAY_OVERRIDE_VALUE = "override";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSubnetIgwExplorer.class);

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String IGW_PREFIX = "igw-";

    private static final String VPCE_PREFIX = "vpce-";

    private static final String TRIAL_DESTINATION_PREFIX = "pl-";

    public boolean hasInternetGatewayOrVpceOfSubnet(List<RouteTable> routeTables, String subnetId, String vpcId, List<VpcEndpoint> vpcEndpoints,
            boolean hasTrialEntitlement) {
        Optional<RouteTable> routeTable = getRouteTableForSubnet(routeTables, subnetId, vpcId);
        LOGGER.debug("Route table for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeTable);

        Optional<Route> routeWithInternetGatewayOrVpce = getRouteWithInternetGatewayOrVPCE(routeTable, hasTrialEntitlement);
        LOGGER.debug("Route with IGW or trial setup for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeWithInternetGatewayOrVpce);

        if (routeWithInternetGatewayOrVpce.isPresent()) {
            return true;
        }

        boolean hasIgwThroughGwlb = checkIgwThroughGwlbVpcEndpoint(routeTable, routeTables, vpcId, vpcEndpoints);
        LOGGER.debug("Subnet '{}' has IGW through GWLB: {}", subnetId, hasIgwThroughGwlb);

        return hasIgwThroughGwlb;
    }

    private boolean checkIgwThroughGwlbVpcEndpoint(Optional<RouteTable> routeTable, List<RouteTable> allRouteTables,
            String vpcId, List<VpcEndpoint> vpcEndpoints) {
        if (routeTable.isEmpty()) {
            return false;
        }

        Set<String> vpceIdsInRoute = routeTable.get().routes().stream()
                .filter(route -> OPEN_CIDR_BLOCK.equals(route.destinationCidrBlock()))
                .filter(route -> StringUtils.isNotEmpty(route.gatewayId()) && route.gatewayId().startsWith(VPCE_PREFIX))
                .map(Route::gatewayId)
                .collect(Collectors.toSet());

        LOGGER.debug("Found VPCE IDs with 0.0.0.0/0 route: {}", vpceIdsInRoute);

        if (vpceIdsInRoute.isEmpty()) {
            return false;
        }

        List<VpcEndpoint> gwlbEndpoints = filterGatewayLoadBalancerEndpoints(vpceIdsInRoute, vpcEndpoints);

        if (gwlbEndpoints.isEmpty()) {
            LOGGER.debug("No Gateway Load Balancer type endpoints found among VPCEs: {}", vpceIdsInRoute);
            return false;
        }

        Set<String> endpointSubnetIds = gwlbEndpoints.stream()
                .flatMap(endpoint -> endpoint.subnetIds().stream())
                .collect(Collectors.toSet());

        if (endpointSubnetIds.isEmpty()) {
            LOGGER.debug("No subnets found for GWLB endpoints");
            return false;
        }

        return isGwlbSubnetsHaveIgwAccess(endpointSubnetIds, allRouteTables, vpcId);
    }

    private List<VpcEndpoint> filterGatewayLoadBalancerEndpoints(Set<String> vpceIds, List<VpcEndpoint> vpcEndpoints) {
        if (vpceIds.isEmpty() || vpcEndpoints == null || vpcEndpoints.isEmpty()) {
            return List.of();
        }

        return vpcEndpoints.stream()
                .filter(endpoint -> vpceIds.contains(endpoint.vpcEndpointId()))
                .filter(endpoint -> VpcEndpointType.GATEWAY_LOAD_BALANCER.equals(endpoint.vpcEndpointType()))
                .collect(Collectors.toList());
    }

    private boolean isGwlbSubnetsHaveIgwAccess(Set<String> subnetIds, List<RouteTable> allRouteTables, String vpcId) {
        for (String subnetId : subnetIds) {
            Optional<RouteTable> subnetRouteTable = getRouteTableForSubnet(allRouteTables, subnetId, vpcId);

            if (subnetRouteTable.isPresent()) {
                boolean hasIgw = subnetRouteTable.get().routes().stream()
                        .anyMatch(this::isInternetGatewayConfigured);

                if (hasIgw) {
                    LOGGER.debug("Firewall subnet '{}' has IGW route", subnetId);
                    return true;
                }
            }
        }

        LOGGER.debug("None of the firewall subnets have IGW routes");
        return false;
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

    private Optional<Route> getRouteWithInternetGatewayOrVPCE(Optional<RouteTable> routeTable, boolean hasTrialEntitlement) {
        Optional<Route> returnRoute = routeTable.stream()
                .flatMap(rt -> rt.routes().stream())
                .filter(route -> isInternetGatewayConfigured(route))
                .findFirst();
        if (returnRoute.isEmpty() && hasTrialEntitlement) {
            // Trial setup
            Set<Route> collectVpces = routeTable.stream()
                    .flatMap(rt -> rt.routes().stream())
                    .filter(route -> isVpceConfigured(route))
                    .filter(route -> isVpceOpenConfigured(route) || isVpceHasDestinationRuleConfigured(route))
                    .collect(Collectors.toSet());
            if (collectVpces.size() == 2) {
                returnRoute = collectVpces.stream().findFirst();
            }
        }
        return returnRoute;
    }

    private boolean isInternetGatewayConfigured(Route route) {
        return StringUtils.isNotEmpty(route.gatewayId())
                && route.gatewayId().startsWith(IGW_PREFIX)
                && OPEN_CIDR_BLOCK.equals(route.destinationCidrBlock());
    }

    private boolean isVpceConfigured(Route route) {
        return StringUtils.isNotEmpty(route.gatewayId())
                && route.gatewayId().startsWith(VPCE_PREFIX);
    }

    private boolean isVpceOpenConfigured(Route route) {
        return OPEN_CIDR_BLOCK.equals(route.destinationCidrBlock());
    }

    private boolean isVpceHasDestinationRuleConfigured(Route route) {
        return StringUtils.isNotEmpty(route.destinationPrefixListId())
                && route.destinationPrefixListId().startsWith(TRIAL_DESTINATION_PREFIX);
    }
}
