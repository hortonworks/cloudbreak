package com.sequenceiq.environment.network.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Calculates maximum 8 subnet CIDRs with netmask 19 from network CIDR with mask 16.
 */
@Component
public class ExtendedSubnetCidrProvider implements SubnetCidrProvider {

    private static final int NUMBER_OF_SUBNETS = 6;

    private static final int PLUS_BITS = 32;

    private static final String SUBNET_MASK = "/19";

    @Override
    public Set<String> provide(String networkCidr) {
        String[] ip = getIp(networkCidr);
        Set<String> subnetCidrs = new HashSet<>();
        for (int i = 0; i < NUMBER_OF_SUBNETS; i++) {
            subnetCidrs.add(String.join(".", ip) + SUBNET_MASK);
            ip[2] = String.valueOf(Integer.parseInt(ip[2]) + PLUS_BITS);
        }
        return subnetCidrs;
    }

    private String[] getIp(String networkCidr) {
        return networkCidr.split("/")[0].split("\\.");
    }

}
