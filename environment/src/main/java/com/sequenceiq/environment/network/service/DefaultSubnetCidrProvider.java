package com.sequenceiq.environment.network.service;

import static com.sequenceiq.environment.network.service.Cidrs.cidrs;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;

@Component
public class DefaultSubnetCidrProvider implements SubnetCidrProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubnetCidrProvider.class);

    private static final int CIDR_PREFIX = 24;

    private static final int INCREMENT_HOST_NUM = 256;

    private static final int SUBNETS = 3;

    @Override
    public Cidrs provide(String networkCidr, boolean privateSubnetEnabled) {
        Set<NetworkSubnetRequest> result = new HashSet<>();

        for (int i = 0; i < SUBNETS; i++) {
            String subnet = calculateSubnet(networkCidr, result);
            result.add(new NetworkSubnetRequest(subnet, SubnetType.PUBLIC));
        }
        return cidrs(result, Sets.newHashSet());
    }

    @Override
    public String cloudPlatform() {
        return "DEFAULT";
    }

    private String calculateSubnet(String networkCidr, Iterable<NetworkSubnetRequest> subnetCidrs) {
        SubnetUtils.SubnetInfo vpcInfo = new SubnetUtils(networkCidr).getInfo();
        String[] cidrParts = vpcInfo.getCidrSignature().split("/");
        int netmask = Integer.parseInt(cidrParts[cidrParts.length - 1]);
        int netmaskBits = CIDR_PREFIX - netmask;
        if (netmaskBits <= 0) {
            throw new CloudConnectorException("The selected VPC has to be in a bigger CIDR range than /24");
        }
        int numberOfSubnets = (int) Math.pow(2, netmaskBits);
        String cidr = getSubnetCidrInRange(networkCidr, subnetCidrs, 0, numberOfSubnets);
        if (cidr == null) {
            throw new CloudConnectorException("Cannot find non-overlapping CIDR range");
        }
        return cidr;
    }

    private String getSubnetCidrInRange(String networkCidr, Iterable<NetworkSubnetRequest> subnetCidrs, int start, int end) {
        SubnetUtils.SubnetInfo vpcInfo = new SubnetUtils(networkCidr).getInfo();
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
            for (NetworkSubnetRequest subnetCidr : subnetCidrs) {
                SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(subnetCidr.getCidr()).getInfo();
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
            LOGGER.debug("The following subnet cidr found: {} for VPC: {}", subnet, networkCidr);
            return subnet;
        } else {
            return null;
        }
    }

    private boolean isInRange(String address, SubnetUtils.SubnetInfo subnetInfo) {
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
