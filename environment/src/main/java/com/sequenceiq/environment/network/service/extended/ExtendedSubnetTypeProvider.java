package com.sequenceiq.environment.network.service.extended;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;

@Component
public class ExtendedSubnetTypeProvider {

    public static final int PLUS_BITS_FOR_24_MASK = 1;

    public static final int PLUS_BITS_FOR_19_MASK = 32;

    public static final int IPV4_ELEMENTS = 4;

    public void updateCidrAndAddToList(int offset, int count, int step, String[] ip, Set<NetworkSubnetRequest> subnetRequests,
            SubnetType subnetType, String subnetMask) {
        for (int i = 0; i < count; i++) {
            int newIpPart = i * step + offset;
            ip[2] = String.valueOf(newIpPart);
            String cidr = String.join(".", ip) + "/" + subnetMask;
            subnetRequests.add(new NetworkSubnetRequest(cidr, subnetType));
        }
    }

    public String[] getIp(String networkCidr) {
        String[] ipAndMask = getIpAndMask(networkCidr);
        String[] ip = ipAndMask[0].split("\\.");
        if (ip.length != IPV4_ELEMENTS) {
            throw new IllegalArgumentException("Invalid networkCidr! " + networkCidr);
        }
        return ip;
    }

    private String[] getIpAndMask(String networkCidr) {
        String[] ipAndMask = networkCidr.split("/");
        if (ipAndMask.length != 2) {
            throw new IllegalArgumentException("Invalid networkCidr! " + networkCidr);
        }
        return ipAndMask;
    }
}
