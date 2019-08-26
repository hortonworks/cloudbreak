package com.sequenceiq.freeipa.api.v1.dns.doc;

public final class DnsOperationDescriptions {
    public static final String ADD_DNS_ZONE_FOR_SUBNETS = "Creates reverse DNS Zone entry for subnets in CIDR format";
    public static final String ADD_DNS_ZONE_FOR_SUBNET_IDS = "Creates reverse DNS Zone entry for subnet IDs";
    public static final String LIST_DNS_ZONES = "List DNS zones available in FreeIPA";
    public static final String DELETE_DNS_ZONE_BY_SUBNET = "Deletes reverse DNS Zone entry by subnet CIDR";
    public static final String DELETE_DNS_ZONE_BY_SUBNET_ID = "Deletes reverse DNS Zone entry by subnet ID";
    public static final String DELETE_DNS_RECORD_BY_FQDN = "Deletes all related A and PTR DNS record";

    private DnsOperationDescriptions() {
    }
}
