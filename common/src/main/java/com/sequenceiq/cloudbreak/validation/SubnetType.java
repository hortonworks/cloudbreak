package com.sequenceiq.cloudbreak.validation;

public enum SubnetType {

    // RFC 1918 valid CIDR-s:
    // 24-bit block 10.0.0.0 - 10.255.255.255
    // 20-bit block 172.16.0.0 - 172.31.255.255
    // 16-bit block 192.168.0.0 - 192.168.255.255
    RFC_1918_COMPLIANT_ONLY,
    // Any valid CIDR-s
    CUSTOM
}
