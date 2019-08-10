package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.RouteTable;

@Component
public class AwsSubnetIgwExplorer {

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    public boolean hasInternetGatewayOfSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId) {
        Optional<RouteTable> routeTable = getRouteTableForSubnet(describeRouteTablesResult, subnetId);
        return routeTable.map(this::hasInternetGateway).orElse(false);
    }

    private Optional<RouteTable> getRouteTableForSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId) {
        List<RouteTable> routeTables = describeRouteTablesResult.getRouteTables();
        return routeTables.stream().filter(rt -> {
            return rt.getAssociations().stream().anyMatch(rta -> Objects.equals(subnetId, rta.getSubnetId()));
        }).findFirst();
    }

    private boolean hasInternetGateway(RouteTable rt) {
        return rt.getRoutes().stream().anyMatch(route -> StringUtils.isNotEmpty(route.getGatewayId())
                && OPEN_CIDR_BLOCK.equals(route.getDestinationCidrBlock()));
    }
}
