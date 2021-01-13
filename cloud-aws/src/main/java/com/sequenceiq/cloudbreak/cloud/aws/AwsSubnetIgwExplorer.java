package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final String IGW_PREFIX = "igw-";

    public boolean hasInternetGatewayOfSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId, String vpcId) {
        Set<RouteTable> routeTables = getRouteTableForSubnet(describeRouteTablesResult, subnetId, vpcId);
        return hasInternetGateway(routeTables, subnetId, vpcId);
    }

    private Set<RouteTable> getRouteTableForSubnet(DescribeRouteTablesResult describeRouteTablesResult, String subnetId, String vpcId) {
        List<RouteTable> routeTables = describeRouteTablesResult.getRouteTables();
        Set<RouteTable> connectedRouteTables = new HashSet<>();
        for (RouteTable rt : routeTables) {
            if (rt.getVpcId().equalsIgnoreCase(vpcId) || rt.getVpcId().startsWith(vpcId)) {
                LOGGER.debug("Analyzing the route table('{}') for the VPC id is:'{}' and the subnet is :'{}'", rt, vpcId, subnetId);
                for (RouteTableAssociation association : rt.getAssociations()) {
                    LOGGER.debug("Analyzing the association('{}') for the VCP id is:'{}' and the subnet is :'{}'", association, vpcId, subnetId);
                    if (StringUtils.isEmpty(association.getSubnetId()) && association.isMain()) {
                        LOGGER.debug("Found a route table('{}') which is 'Main'/default for the VPC('{}'), "
                                + "doesn't need to check the subnet id as it is not returned in this case", rt, vpcId);
                        connectedRouteTables.add(rt);
                    } else if (subnetId.equalsIgnoreCase(association.getSubnetId())
                            || (StringUtils.isNotEmpty(association.getSubnetId()) && association.getSubnetId().startsWith(subnetId))) {
                        LOGGER.info("Found the route table('{}') which is explicitly connected to the subnet('{}')", rt, subnetId);
                        connectedRouteTables.add(rt);
                        break;
                    }
                }
            }
        }
        return connectedRouteTables;
    }

    private boolean hasInternetGateway(Set<RouteTable> routeTables, String subnetId, String vpcId) {
        if (!routeTables.isEmpty()) {
            for (RouteTable routeTable : routeTables) {
                for (Route route : routeTable.getRoutes()) {
                    LOGGER.debug("Searching the route which is open. the route is {} and the subnet is :{}", route, subnetId);
                    if (StringUtils.isNotEmpty(route.getGatewayId()) && route.getGatewayId().startsWith(IGW_PREFIX)
                            && OPEN_CIDR_BLOCK.equals(route.getDestinationCidrBlock())) {
                        LOGGER.info("Found the route('{}') with internet gateway for the subnet ('{}') within VPC('{}')", route, subnetId, vpcId);
                        return true;
                    }
                }
            }
        }
        LOGGER.info("Internet gateway with route that has '{}' as destination CIDR block could not be found for subnet('{}') within VPC('{}')",
                OPEN_CIDR_BLOCK, subnetId, vpcId);
        return false;
    }
}
