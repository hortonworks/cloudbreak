package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.singletonList;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Service
public class AwsNetworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkService.class);

    private static final int CIDR_PREFIX = 24;

    private static final int INCREMENT_HOST_NUM = 256;

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    @Inject
    private AwsClient awsClient;

    public List<Group> getGatewayGroups(Collection<Group> groups) {
        return groups.stream().filter(group -> group.getType() == InstanceGroupType.GATEWAY).collect(Collectors.toList());
    }

    public List<String> getExistingSubnetCidr(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);
        DescribeSubnetsRequest subnetsRequest = new DescribeSubnetsRequest().withSubnetIds(awsNetworkView.getSubnetList());
        List<Subnet> subnets = ec2Client.describeSubnets(subnetsRequest).getSubnets();
        if (subnets.isEmpty()) {
            throw new CloudConnectorException("The specified subnet does not exist (maybe it's in a different region).");
        }
        List<String> cidrs = Lists.newArrayList();
        for (Subnet subnet : subnets) {
            cidrs.add(subnet.getCidrBlock());
        }
        return cidrs;
    }

    public boolean isMapPublicOnLaunch(AwsNetworkView awsNetworkView, AmazonEC2 amazonEC2Client) {
        boolean mapPublicIpOnLaunch = true;
        if (awsNetworkView.isExistingVPC() && awsNetworkView.isExistingSubnet()) {
            DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
            describeSubnetsRequest.setSubnetIds(awsNetworkView.getSubnetList());
            DescribeSubnetsResult describeSubnetsResult = amazonEC2Client.describeSubnets(describeSubnetsRequest);
            if (!describeSubnetsResult.getSubnets().isEmpty()) {
                mapPublicIpOnLaunch = describeSubnetsResult.getSubnets().get(0).isMapPublicIpOnLaunch();
            }
        }
        return mapPublicIpOnLaunch;
    }

    public String findNonOverLappingCIDR(AuthenticatedContext ac, CloudStack stack) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);

        DescribeVpcsRequest vpcRequest = new DescribeVpcsRequest().withVpcIds(awsNetworkView.getExistingVPC());
        Vpc vpc = ec2Client.describeVpcs(vpcRequest).getVpcs().get(0);
        String vpcCidr = vpc.getCidrBlock();
        LOGGER.debug("Subnet cidr is empty, find a non-overlapping subnet for VPC cidr: {}", vpcCidr);

        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withFilters(new Filter("vpc-id", singletonList(awsNetworkView.getExistingVPC())));
        List<Subnet> awsSubnets = ec2Client.describeSubnets(request).getSubnets();
        List<String> subnetCidrs = awsSubnets.stream().map(Subnet::getCidrBlock).collect(Collectors.toList());
        LOGGER.debug("The selected VPCs: {}, has the following subnets: {}", vpc.getVpcId(), String.join(",", subnetCidrs));

        return calculateSubnet(ac.getCloudContext().getName(), vpc, subnetCidrs);
    }

    private String calculateSubnet(String stackName, Vpc vpc, Iterable<String> subnetCidrs) {
        SubnetInfo vpcInfo = new SubnetUtils(vpc.getCidrBlock()).getInfo();
        String[] cidrParts = vpcInfo.getCidrSignature().split("/");
        int netmask = Integer.parseInt(cidrParts[cidrParts.length - 1]);
        int netmaskBits = CIDR_PREFIX - netmask;
        if (netmaskBits <= 0) {
            throw new CloudConnectorException("The selected VPC has to be in a bigger CIDR range than /24");
        }
        int numberOfSubnets = Double.valueOf(Math.pow(2, netmaskBits)).intValue();
        int targetSubnet = 0;
        if (stackName != null) {
            byte[] b = stackName.getBytes(Charset.forName("UTF-8"));
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
        SubnetInfo vpcInfo = new SubnetUtils(vpc.getCidrBlock()).getInfo();
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
            LOGGER.debug("The following subnet cidr found: {} for VPC: {}", subnet, vpc.getVpcId());
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
}
