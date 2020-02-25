package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RouteTableAssociation;

@Component
public class AwsSubnetIgwExplorer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSubnetIgwExplorer.class);

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    public boolean hasInternetGatewayOfSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId) {
        Optional<RouteTable> routeTable = getRouteTableForSubnet(describeRouteTablesResult, subnetId);
        return hasInternetGateway(routeTable, subnetId);
    }

    private Optional<RouteTable> getRouteTableForSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId) {
        List<RouteTable> routeTables = describeRouteTablesResult.getRouteTables();
        Optional<RouteTable> routeTable = Optional.empty();
        for (RouteTable rt : routeTables) {
            LOGGER.info("Searching the routeTable where routeTable is {} and the subnet is :{}", rt, subnetId);
            for (RouteTableAssociation association : rt.getAssociations()) {
                LOGGER.info("Searching the association where association is {} and the subnet is :{}", association, subnetId);
                if (subnetId.equalsIgnoreCase(association.getSubnetId())) {
                    LOGGER.info("Found the routeTable which is {} and the subnet is :{}", rt, subnetId);
                    routeTable = Optional.ofNullable(rt);
                    break;
                }
            }

            if (routeTable.isPresent()) {
                break;
            }
        }
        return routeTable;
    }

    private boolean hasInternetGateway(Optional<RouteTable> rt, String subnetId) {
        if (rt.isPresent()) {
            for (Route route : rt.get().getRoutes()) {
                LOGGER.info("Searching the route which is open. the route is {} and the subnet is :{}", route, subnetId);
                if (StringUtils.isNotEmpty(route.getGatewayId()) && OPEN_CIDR_BLOCK.equals(route.getDestinationCidrBlock())) {
                    LOGGER.info("Found the route which is {} and the subnet is :{}", route, subnetId);
                    return true;
                }
            }
        }
        return false;
    }
}
