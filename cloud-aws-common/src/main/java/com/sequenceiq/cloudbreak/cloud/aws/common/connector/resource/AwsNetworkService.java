package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.PrefixList;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcCidrBlockAssociation;

@Service
public class AwsNetworkService {

    public static final String VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN = "com.amazonaws.%s.%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkService.class);

    private static final int CIDR_PREFIX = 24;

    private static final int INCREMENT_HOST_NUM = 256;

    @Inject
    private CommonAwsClient awsClient;

    @Value("${cb.aws.vpcendpoints.enabled.gateway.services}")
    private Set<String> enabledGatewayServices;

    public List<Group> getGatewayGroups(Collection<Group> groups) {
        return groups.stream().filter(group -> group.getType() == InstanceGroupType.GATEWAY).collect(Collectors.toList());
    }

    public List<String> getExistingSubnetCidr(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeSubnetsRequest subnetsRequest = DescribeSubnetsRequest.builder().subnetIds(awsNetworkView.getSubnetList()).build();
        List<Subnet> subnets = ec2Client.describeSubnets(subnetsRequest).subnets();
        if (subnets.isEmpty()) {
            throw new CloudConnectorException("The specified subnet does not exist (maybe it's in a different region).");
        }
        List<String> cidrs = Lists.newArrayList();
        for (Subnet subnet : subnets) {
            cidrs.add(subnet.cidrBlock());
        }
        return cidrs;
    }

    public boolean isMapPublicOnLaunch(AwsNetworkView awsNetworkView, AmazonEc2Client amazonEC2Client) {
        boolean mapPublicIpOnLaunch = true;
        if (awsNetworkView.isExistingVPC() && awsNetworkView.isExistingSubnet()) {
            mapPublicIpOnLaunch = isMapPublicOnLaunch(awsNetworkView.getSubnetList(), amazonEC2Client);
        }
        return mapPublicIpOnLaunch;
    }

    public boolean isMapPublicOnLaunch(List<String> subnetIds, AmazonEc2Client amazonEC2Client) {
        boolean mapPublicIpOnLaunch = true;
        DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder().subnetIds(subnetIds).build();
        DescribeSubnetsResponse describeSubnetsResponse = amazonEC2Client.describeSubnets(describeSubnetsRequest);
        if (!describeSubnetsResponse.subnets().isEmpty()) {
            mapPublicIpOnLaunch = describeSubnetsResponse.subnets().get(0).mapPublicIpOnLaunch();
            LOGGER.debug("The {} subnet is mapPublicIpOnLaunch: {}", describeSubnetsResponse.subnets().get(0).subnetId(), mapPublicIpOnLaunch);
        }
        return mapPublicIpOnLaunch;
    }

    public String findNonOverLappingCIDR(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()), region);

        DescribeVpcsRequest vpcRequest = DescribeVpcsRequest.builder().vpcIds(awsNetworkView.getExistingVpc()).build();
        Vpc vpc = ec2Client.describeVpcs(vpcRequest).vpcs().get(0);
        String vpcCidr = vpc.cidrBlock();
        LOGGER.debug("Subnet cidr is empty, find a non-overlapping subnet for VPC cidr: {}", vpcCidr);

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .filters(Filter.builder().name("vpc-id").values(awsNetworkView.getExistingVpc()).build())
                .build();
        List<Subnet> awsSubnets = ec2Client.describeSubnets(request).subnets();
        List<String> subnetCidrs = awsSubnets.stream().map(Subnet::cidrBlock).collect(Collectors.toList());
        LOGGER.debug("The selected VPCs: {}, has the following subnets: {}", vpc.vpcId(), String.join(",", subnetCidrs));

        return calculateSubnet(ac.getCloudContext().getName(), vpc, subnetCidrs);
    }

    public List<String> getVpcCidrs(AuthenticatedContext ac, AwsNetworkView awsNetworkView) {
        if (awsNetworkView.isExistingVPC()) {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            AmazonEc2Client ec2Client = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()), region);

            DescribeVpcsRequest vpcRequest = DescribeVpcsRequest.builder().vpcIds(awsNetworkView.getExistingVpc()).build();
            Vpc vpc = ec2Client.describeVpcs(vpcRequest).vpcs().get(0);
            List<String> cidrBlockAssociationSet = vpc.cidrBlockAssociationSet().stream()
                    .map(VpcCidrBlockAssociation::cidrBlock)
                    .collect(Collectors.toList());
            LOGGER.info("VPC associated CIDR blocks: [{}]", cidrBlockAssociationSet);
            return cidrBlockAssociationSet;
        } else {
            return Collections.emptyList();
        }
    }

    private String calculateSubnet(String stackName, Vpc vpc, Iterable<String> subnetCidrs) {
        SubnetInfo vpcInfo = new SubnetUtils(vpc.cidrBlock()).getInfo();
        String[] cidrParts = vpcInfo.getCidrSignature().split("/");
        int netmask = Integer.parseInt(cidrParts[cidrParts.length - 1]);
        int netmaskBits = CIDR_PREFIX - netmask;
        if (netmaskBits <= 0) {
            throw new CloudConnectorException("The selected VPC has to be in a bigger CIDR range than /24");
        }
        int numberOfSubnets = Double.valueOf(StrictMath.pow(2, netmaskBits)).intValue();
        int targetSubnet = 0;
        if (stackName != null) {
            byte[] b = stackName.getBytes(StandardCharsets.UTF_8);
            for (byte ascii : b) {
                targetSubnet += ascii;
            }
        }
        targetSubnet = Long.valueOf(targetSubnet % numberOfSubnets).intValue();
        String cidr = getSubnetCidrInRange(vpc, subnetCidrs, targetSubnet, numberOfSubnets);
        if (cidr == null) {
            cidr = getSubnetCidrInRange(vpc, subnetCidrs, 0, targetSubnet);
        }
        if (cidr == null) {
            throw new CloudConnectorException("Cannot find non-overlapping CIDR range");
        }
        return cidr;
    }

    private String getSubnetCidrInRange(Vpc vpc, Iterable<String> subnetCidrs, int start, int end) {
        SubnetInfo vpcInfo = new SubnetUtils(vpc.cidrBlock()).getInfo();
        String lowProbe = incrementIp(vpcInfo.getLowAddress());
        String highProbe = new SubnetUtils(toSubnetCidr(lowProbe)).getInfo().getHighAddress();
        // start from the target subnet
        for (int i = 0; i < start - 1; i++) {
            lowProbe = incrementIp(lowProbe);
            highProbe = incrementIp(highProbe);
        }
        boolean foundProbe = false;
        for (int i = start; i < end; i++) {
            boolean overlapping = false;
            for (String subnetCidr : subnetCidrs) {
                SubnetInfo subnetInfo = new SubnetUtils(subnetCidr).getInfo();
                if (isInRange(lowProbe, subnetInfo) || isInRange(highProbe, subnetInfo)) {
                    overlapping = true;
                    break;
                }
            }
            if (overlapping) {
                lowProbe = incrementIp(lowProbe);
                highProbe = incrementIp(highProbe);
            } else {
                foundProbe = true;
                break;
            }
        }
        if (foundProbe && isInRange(highProbe, vpcInfo)) {
            String subnet = toSubnetCidr(lowProbe);
            LOGGER.debug("The following subnet cidr found: {} for VPC: {}", subnet, vpc.vpcId());
            return subnet;
        } else {
            return null;
        }
    }

    private boolean isInRange(String address, SubnetInfo subnetInfo) {
        int low = InetAddresses.coerceToInteger(InetAddresses.forString(subnetInfo.getLowAddress()));
        int high = InetAddresses.coerceToInteger(InetAddresses.forString(subnetInfo.getHighAddress()));
        int currentAddress = InetAddresses.coerceToInteger(InetAddresses.forString(address));
        return low <= currentAddress && currentAddress <= high;
    }

    private String incrementIp(String ip) {
        int ipValue = InetAddresses.coerceToInteger(InetAddresses.forString(ip)) + INCREMENT_HOST_NUM;
        return InetAddresses.fromInteger(ipValue).getHostAddress();
    }

    private String toSubnetCidr(String ip) {
        int ipValue = InetAddresses.coerceToInteger(InetAddresses.forString(ip)) - 1;
        return InetAddresses.fromInteger(ipValue).getHostAddress() + "/24";
    }

    public List<String> getPrefixListIds(AmazonEc2Client amazonEC2Client, String regionName, OutboundInternetTraffic outboundInternetTraffic) {
        List<String> result = List.of();
        if (outboundInternetTraffic == OutboundInternetTraffic.DISABLED && CollectionUtils.isNotEmpty(enabledGatewayServices)) {
            Set<String> gatewayRegionServices = enabledGatewayServices.stream()
                    .map(s -> String.format(VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, regionName, s))
                    .collect(Collectors.toSet());
            result = amazonEC2Client.describePrefixLists().prefixLists().stream()
                    .filter(pl -> gatewayRegionServices.contains(pl.prefixListName()))
                    .map(PrefixList::prefixListId)
                    .collect(Collectors.toList());
        }
        return result;
    }
}
