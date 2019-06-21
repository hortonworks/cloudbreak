package com.sequenceiq.freeipa.service.dns;

import org.springframework.stereotype.Component;

@Component
public class ReverseDnsZoneCalculator {

    public static final String IN_ADDR_ARPA = ".in-addr.arpa.";

    public static final int C_CLASS_ADDRESS = 24;

    public static final int B_CLASS_ADDRESS = 16;

    public String reverseDnsZoneForCidr(String cidr) {
        if (!cidr.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\/([0-9]|[1-2][0-9]|3[0-2]))$")) {
            throw new ReverseDnsZoneCalculatorException(String.format("CIDR [%s] is not a valid", cidr));
        }
        String[] subnetAndMask = cidr.split("/");
        String[] octetts = subnetAndMask[0].split("\\.");
        int mask = Integer.parseInt(subnetAndMask[1]);
        StringBuffer reverseDnsZone = new StringBuffer();
        if (mask >= C_CLASS_ADDRESS) {
            reverseDnsZone.append(octetts[2]).append('.').append(octetts[1]).append('.').append(octetts[0]).append(IN_ADDR_ARPA);
        } else if (mask >= B_CLASS_ADDRESS) {
            reverseDnsZone.append(octetts[1]).append('.').append(octetts[0]).append(IN_ADDR_ARPA);
        } else {
            reverseDnsZone.append(octetts[0]).append(IN_ADDR_ARPA);
        }
        return reverseDnsZone.toString();
    }

}
