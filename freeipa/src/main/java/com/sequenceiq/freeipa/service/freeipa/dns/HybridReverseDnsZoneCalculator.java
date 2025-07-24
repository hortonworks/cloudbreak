package com.sequenceiq.freeipa.service.freeipa.dns;

import static com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator.A_CLASS_ADDRESS;
import static com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator.B_CLASS_ADDRESS;
import static com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator.CIDR_REGEX;
import static com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator.C_CLASS_ADDRESS;
import static com.sequenceiq.freeipa.service.freeipa.dns.ReverseDnsZoneCalculator.IN_ADDR_ARPA;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HybridReverseDnsZoneCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridReverseDnsZoneCalculator.class);

    // 0b11111111
    private static final int OCTET_MAX_VALUE = 255;

    public String reverseDnsZoneForCidrs(Collection<String> cidrs) {
        return String.join(",", reverseDnsZoneForCidrsAsSet(cidrs));
    }

    public Set<String> reverseDnsZoneForCidrsAsSet(Collection<String> cidrs) {
        return cidrs.stream()
                .map(this::reverseDnsZoneForCidr)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Set<String> reverseDnsZoneForCidr(String cidr) {
        LOGGER.info("Calculate reverse zones for cidr: {}", cidr);
        validateCidr(cidr);
        String[] subnetAndMask = cidr.split("/");
        String[] octets = subnetAndMask[0].split("\\.");
        int mask = Integer.parseInt(subnetAndMask[1]);
        if (mask == A_CLASS_ADDRESS ||  mask == B_CLASS_ADDRESS ||  mask >= C_CLASS_ADDRESS) {
            Set<String> zones = Set.of(handleClassfulAddress(mask, octets));
            LOGGER.info("Reverse zones for classful cidr: {}, zones: {}", cidr, zones);
            return zones;
        } else {
            SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(cidr).getInfo();
            String netmaskDotSeparated = subnetInfo.getNetmask();
            String[] netmaskOctets = netmaskDotSeparated.split("\\.");
            Set<String> zones = new HashSet<>();
            if (mask < A_CLASS_ADDRESS) {
                for (int i = calculateMin(netmaskOctets[0], octets[0]); i <= calculateMax(netmaskOctets[0], octets[0]); i++) {
                    zones.add(i + IN_ADDR_ARPA);
                }
            } else if (mask < B_CLASS_ADDRESS) {
                for (int i = calculateMin(netmaskOctets[1], octets[1]); i <= calculateMax(netmaskOctets[1], octets[1]); i++) {
                    zones.add(String.valueOf(i) + '.' + octets[0] + IN_ADDR_ARPA);
                }
            } else {
                for (int i = calculateMin(netmaskOctets[2], octets[2]); i <= calculateMax(netmaskOctets[2], octets[2]); i++) {
                    zones.add(String.valueOf(i) + '.' + octets[1] + '.' + octets[0] + IN_ADDR_ARPA);
                }
            }
            LOGGER.info("Reverse zones for classless cidr: {}, zones: {}", cidr, zones);
            return zones;
        }
    }

    private void validateCidr(String cidr) {
        if (!cidr.matches(CIDR_REGEX)) {
            throw new ReverseDnsZoneCalculatorException(String.format("[%s] is not a valid CIDR", cidr));
        }
    }

    private int calculateMin(String maskPart, String octet) {
        int ipPart = Integer.parseInt(octet);
        int mask = Integer.parseInt(maskPart);
        return ipPart & mask;
    }

    private int calculateMax(String maskPart, String octet) {
        int mask = Integer.parseInt(maskPart);
        int inverseMaskPart = mask ^ OCTET_MAX_VALUE;
        int ipPart = Integer.parseInt(octet);
        return ipPart | inverseMaskPart;
    }

    private String handleClassfulAddress(int mask, String[] octets) {
        StringBuilder reverseDnsZone = new StringBuilder();
        if (mask >= C_CLASS_ADDRESS) {
            reverseDnsZone.append(octets[2]).append('.').append(octets[1]).append('.').append(octets[0]).append(IN_ADDR_ARPA);
        } else if (mask == B_CLASS_ADDRESS) {
            reverseDnsZone.append(octets[1]).append('.').append(octets[0]).append(IN_ADDR_ARPA);
        } else if (mask == A_CLASS_ADDRESS) {
            reverseDnsZone.append(octets[0]).append(IN_ADDR_ARPA);
        }
        return reverseDnsZone.toString();
    }

}
