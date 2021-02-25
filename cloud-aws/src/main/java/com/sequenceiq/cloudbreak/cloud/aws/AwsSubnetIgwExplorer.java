package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Route;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.networkfirewall.model.AWSNetworkFirewallException;
import com.amazonaws.services.networkfirewall.model.DescribeFirewallRequest;
import com.amazonaws.services.networkfirewall.model.FirewallMetadata;
import com.amazonaws.services.networkfirewall.model.ListFirewallsRequest;
import com.amazonaws.services.networkfirewall.model.SyncState;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonNetworkFirewallClient;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;

@Component
public class AwsSubnetIgwExplorer {

    static final String ENDPOINT_GATEWAY_OVERRIDE_KEY = "cdp-public-endpoint-gateway-lb";

    static final String ENDPOINT_GATEWAY_OVERRIDE_VALUE = "override";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSubnetIgwExplorer.class);

    private static final String OPEN_CIDR_BLOCK = "0.0.0.0/0";

    private static final String IGW_PREFIX = "igw-";

    private static final String FW_PREFIX = "vpce-";

    private static final String ACCESS_DENIED = "AccessDeniedException";

    public boolean hasInternetGatewayOfSubnet(List<RouteTable> routeTables, String subnetId, String vpcId) {
        Optional<RouteTable> routeTable = getRouteTableForSubnet(routeTables, subnetId, vpcId);
        LOGGER.debug("Route table for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeTable);

        Optional<Route> routeWithInternetGateway = getRouteWithInternetGateway(routeTable);
        LOGGER.debug("Route with IGW for subnet '{}' (VPC is '{}'): '{}'", subnetId, vpcId, routeWithInternetGateway);

        return routeWithInternetGateway.isPresent();
    }

    private Optional<RouteTable> getRouteTableForSubnet(List<RouteTable> tableList, String subnetId, String vpcId) {
        List<RouteTable> routeTables = tableList.stream()
                .filter(rt -> rt.getVpcId().equalsIgnoreCase(vpcId))
                .collect(Collectors.toList());
        LOGGER.debug("Route tables for VPC '{}' (current subnet is '{}'): '{}'", vpcId, subnetId, routeTables);

        Optional<RouteTable> explicitRouteTable = getExplicitRouteTable(routeTables, subnetId);
        LOGGER.debug("Explicit route table for subnet '{}' (VPC is '{}'): {}", subnetId, vpcId, explicitRouteTable);

        return explicitRouteTable.or(() -> {
            LOGGER.debug("There is no explicit route table for subnet '{}' (VPC is '{}') so we should look for the implicit one.", subnetId, vpcId);
            Optional<RouteTable> implicitRouteTable = getImplicitRouteTable(routeTables);
            LOGGER.debug("Implicit route table for subnet '{}' (VPC is '{}'): {}", subnetId, vpcId, implicitRouteTable);
            return implicitRouteTable;
        });
    }

    private Optional<RouteTable> getExplicitRouteTable(List<RouteTable> routeTables, String subnetId) {
        return routeTables.stream()
                .filter(rt -> rt.getAssociations().stream().anyMatch(rta -> subnetId.equalsIgnoreCase(rta.getSubnetId())))
                .findFirst();
    }

    private Optional<RouteTable> getImplicitRouteTable(List<RouteTable> routeTables) {
        return routeTables.stream()
                .filter(rt -> rt.getAssociations().stream().anyMatch(rta -> StringUtils.isEmpty(rta.getSubnetId()) && rta.isMain()))
                .findFirst();
    }

    private Optional<Route> getRouteWithInternetGateway(Optional<RouteTable> routeTable) {
        return routeTable.stream()
                .flatMap(rt -> rt.getRoutes().stream())
                .filter(route -> StringUtils.isNotEmpty(route.getGatewayId()) &&
                        route.getGatewayId().startsWith(IGW_PREFIX) &&
                        OPEN_CIDR_BLOCK.equals(route.getDestinationCidrBlock()))
                .findFirst();
    }

    public boolean isRoutableToInternet(AmazonEc2Client ec2Client, AmazonNetworkFirewallClient nfwClient, List<RouteTable> routeTables,
            String subnetId, String vpcId) {
        LOGGER.info("Checking if subnet {} has the {} override tag.", subnetId, ENDPOINT_GATEWAY_OVERRIDE_KEY);
        boolean result = isEndpointGatewayOverrideTagSet(ec2Client, subnetId);
        if (!result) {
            LOGGER.info("{} tag not set. Checking if subnet has internet gateway explicitly defined in route table.", ENDPOINT_GATEWAY_OVERRIDE_KEY);
            result = hasInternetGatewayOfSubnet(routeTables, subnetId, vpcId);
        }
        if (!result) {
            LOGGER.info("No internet gateway defined in subnet route table. Checking if subnet has route to network firewall with internet gateway.");
            result = isRoutableToFirewallWithInternetGateway(routeTables, subnetId, vpcId, nfwClient);
        }
        if (!result) {
            LOGGER.info("No route to internet found for subnet {}. Subnet cannot be used for Public Endpoint Gateway.", subnetId);
        }
        return result;
    }

    private boolean isEndpointGatewayOverrideTagSet(AmazonEc2Client ec2Client, String subnetId) {
        LOGGER.debug("Issuing DescribeSubnetsRequest for subnet {}", subnetId);
        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withSubnetIds(subnetId);
        DescribeSubnetsResult result = ec2Client.describeSubnets(request);
        List<Tag> tags = result.getSubnets().stream()
            .flatMap(subnet -> subnet.getTags().stream())
            .collect(Collectors.toList());
        LOGGER.debug("Found tags {} for subnet {}", tags, subnetId);
        return tags.stream()
            .anyMatch(tag ->
                ENDPOINT_GATEWAY_OVERRIDE_KEY.equalsIgnoreCase(tag.getKey()) &&
                    ENDPOINT_GATEWAY_OVERRIDE_VALUE.equalsIgnoreCase(tag.getValue())
            );
    }

    private boolean isRoutableToFirewallWithInternetGateway(List<RouteTable> routeTables, String subnetId, String vpcId,
            AmazonNetworkFirewallClient nfwClient) {
        boolean result = false;
        try {
            List<String> gatewayIds = getPotentialFirewallEndpointIds(routeTables, subnetId, vpcId);
            if (!gatewayIds.isEmpty()) {
                LOGGER.info("Found gateway ids {} in route table for subnet {}", gatewayIds, subnetId);
                List<FirewallMetadata> firewallMetadata = getAllFirewallMetadataForVpc(nfwClient, vpcId);
                if (!firewallMetadata.isEmpty()) {
                    LOGGER.info("Found firewalls {} in VPC {}", firewallMetadata.stream().map(FirewallMetadata::getFirewallName), vpcId);
                    result = hasRouteToPublicFirewall(firewallMetadata, routeTables, subnetId, vpcId, nfwClient, gatewayIds);
                } else {
                    LOGGER.info("Did not find any firewalls in VPC {}", vpcId);
                }
            } else {
                LOGGER.info("Did not find any potential firewall routing paths for subnet {}", subnetId);
            }
        } catch (AWSNetworkFirewallException e) {
            if (ACCESS_DENIED.equals(e.getErrorCode())) {
                LOGGER.info("User does not have permission to query firewall resources: {}. Skipping firewall IGW check.",
                    e.getMessage());
            } else {
                LOGGER.warn("Unexpected exception while fetching firewall information. Skipping firewall IGW check.", e);
            }
        }
        return result;
    }

    private List<String> getPotentialFirewallEndpointIds(List<RouteTable> routeTables, String subnetId, String vpcId) {
        List<String> gatewayIds = new ArrayList<>();
        LOGGER.info("Searching subnet {} route table for potential firewall routing", subnetId);
        Optional<RouteTable> subnetRouteTable = getRouteTableForSubnet(routeTables, subnetId, vpcId);
        LOGGER.info("Searching subnet route table for any routes that start with the firewall gateway prefix ({})", FW_PREFIX);
        subnetRouteTable.ifPresent(
            routeTable ->
                gatewayIds.addAll(subnetRouteTable.get().getRoutes().stream()
                    .filter(route -> StringUtils.isNotEmpty(route.getGatewayId()) && route.getGatewayId().startsWith(FW_PREFIX))
                    .map(Route::getGatewayId)
                    .collect(Collectors.toList())));
        return gatewayIds;
    }

    private List<FirewallMetadata> getAllFirewallMetadataForVpc(AmazonNetworkFirewallClient nfwClient, String vpcId) {
        ListFirewallsRequest request = new ListFirewallsRequest()
            .withVpcIds(vpcId);
        return AwsPageCollector.getAllFirewallMetadata(nfwClient, request);
    }

    private boolean hasRouteToPublicFirewall(List<FirewallMetadata> firewallMetadata, List<RouteTable> routeTables,
            String subnetId, String vpcId, AmazonNetworkFirewallClient nfwClient, List<String> gatewayIds) {
        LOGGER.info("Searching firewalls for gateway id in subnet {} route table.", subnetId);
        boolean result = false;
        List<AwsFirewall> firewalls = getFirewalls(firewallMetadata, nfwClient);
        List<SyncState> syncStates = getStateForRoutedFirewalls(firewalls, nfwClient, gatewayIds, subnetId);
        if (!syncStates.isEmpty()) {
            LOGGER.info("Found {} firewall status states that match gateway ids in the subnet {} route table",
                syncStates.size(), subnetId);
            Optional<SyncState> syncStateOptional = syncStates.stream()
                .filter(syncState -> hasInternetGatewayOfSubnet(routeTables, syncState.getAttachment().getSubnetId(), vpcId))
                .findFirst();
            result = syncStateOptional.isPresent();
            if (result) {
                String firewallName = firewalls.stream()
                    .filter(fw -> fw.getAllSyncStates().contains(syncStateOptional.get()) && fw.getName() != null)
                    .map(AwsFirewall::getName)
                    .findAny().orElse("[could not find name]");
                LOGGER.info("Found routed firewall {} with attached internet gateway for subnet {}", firewallName, subnetId);
            } else {
                LOGGER.info("The firewalls used by subnet {} do not have an internet gateway attached.", subnetId);
            }
        } else {
            LOGGER.info("Did not find any firewall metadata that matched the route table for subnet {}", subnetId);
        }
        return result;
    }

    private List<AwsFirewall> getFirewalls(List<FirewallMetadata> firewallMetadata, AmazonNetworkFirewallClient nfwClient) {
        List<String> firewallArns = firewallMetadata.stream()
            .map(FirewallMetadata::getFirewallArn)
            .collect(Collectors.toList());
        LOGGER.debug("Issuing DescribeFirewallRequest for firewalls with arns {}", firewallArns);
        return firewallArns.stream()
            .map(arn -> nfwClient.describeFirewall(new DescribeFirewallRequest().withFirewallArn(arn)))
            .map(result -> new AwsFirewall(result.getFirewall().getFirewallName(), result.getFirewallStatus()))
            .collect(Collectors.toList());
    }

    private List<SyncState> getStateForRoutedFirewalls(List<AwsFirewall> firewalls, AmazonNetworkFirewallClient nfwClient,
            List<String> gatewayIds, String subnetId) {
        LOGGER.info("Searching firewall status for gateway id that matches subnet {} route table.", subnetId);
        return firewalls.stream()
            .flatMap(fw -> fw.getAllSyncStates().stream())
            .filter(syncState -> doesEndpointMatchSubnetGatewayList(syncState, gatewayIds))
            .collect(Collectors.toList());
    }

    private boolean doesEndpointMatchSubnetGatewayList(SyncState syncState, List<String> gatewayids) {
        return gatewayids.contains(syncState.getAttachment().getEndpointId());
    }
}
