package com.sequenceiq.environment.network.service;

import static com.sequenceiq.environment.network.service.Cidrs.cidrs;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Calculates maximum 8 subnet CIDRs with netmask 19 from network CIDR with mask 16.
 */
@Component
public class ExtendedSubnetCidrProvider implements SubnetCidrProvider {

    private static final int NUMBER_OF_PUBLIC_SUBNETS = 3;

    private static final int NUMBER_OF_MLX_SUBNETS = 32;

    private static final String DWX_STARTING_IP = "64";

    private static final int NUMBER_OF_DWX_SUBNETS = 3;

    private static final int PLUS_BITS_24 = 1;

    private static final int PLUS_BITS_19 = 32;

    private static final String PUBLIC_SUBNET_MASK = "/24";

    private static final String MLX_SUBNET_MASK = "/24";

    private static final String DWX_SUBNET_MASK = "/19";

    @Override
    public Cidrs provide(String networkCidr) {
        String[] ip = getIp(networkCidr);
        Set<String> publicSubnetCidrs = new HashSet<>();
        Set<String> privateSubnetCidrs = new HashSet<>();

        for (int i = 0; i < NUMBER_OF_PUBLIC_SUBNETS; i++) {
            publicSubnetCidrs.add(String.join(".", ip) + PUBLIC_SUBNET_MASK);
            ip[2] = String.valueOf(Integer.parseInt(ip[2]) + PLUS_BITS_24);
        }
        for (int i = 0; i < NUMBER_OF_MLX_SUBNETS; i++) {
            privateSubnetCidrs.add(String.join(".", ip) + MLX_SUBNET_MASK);
            ip[2] = String.valueOf(Integer.parseInt(ip[2]) + PLUS_BITS_24);
        }
        // we need to increase this because of the masking and on Azure we need to start from .64
        ip[2] = DWX_STARTING_IP;
        for (int i = 0; i < NUMBER_OF_DWX_SUBNETS; i++) {
            privateSubnetCidrs.add(String.join(".", ip) + DWX_SUBNET_MASK);
            ip[2] = String.valueOf(Integer.parseInt(ip[2]) + PLUS_BITS_19);
        }
        return cidrs(publicSubnetCidrs, privateSubnetCidrs);
    }

    private String[] getIp(String networkCidr) {
        return networkCidr.split("/")[0].split("\\.");
    }

}
