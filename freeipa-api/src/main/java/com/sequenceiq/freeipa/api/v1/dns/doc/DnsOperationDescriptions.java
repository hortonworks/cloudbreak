package com.sequenceiq.freeipa.api.v1.dns.doc;

public final class DnsOperationDescriptions {
    public static final String ADD_DNS_ZONE_FOR_SUBNETS = "Creates reverse DNS Zone entry for subnets in CIDR format";
    public static final String ADD_DNS_ZONE_FOR_SUBNET_IDS = "Creates reverse DNS Zone entry for subnet IDs";
    public static final String LIST_DNS_ZONES = "List DNS zones available in FreeIPA";
    public static final String DELETE_DNS_ZONE_BY_SUBNET = "Deletes reverse DNS Zone entry by subnet CIDR";
    public static final String DELETE_DNS_ZONE_BY_SUBNET_ID = "Deletes reverse DNS Zone entry by subnet ID";
    public static final String DELETE_DNS_RECORD_BY_FQDN = "Deletes all related A and PTR DNS record";
    public static final String ADD_DNS_CNAME_RECORD = "Creates a DNS CNAME record with the value in the defined zone if zone exists. "
            + "If zone not specified default zone will be used.";
    public static final String ADD_DNS_A_RECORD = "Creates a DNS A record with the value in the defined zone if zone exists. If zone not specified "
            + "default zone will be used. Reverse pointer is created if requested and reverse zone exists.";

    public static final String DELETE_DNS_A_RECORD = "Deletes the A record in DNS Zone and tries to delete PTR if exists.";

    public static final String DELETE_DNS_CNAME_RECORD = "Deletes the CNAME record in DNS Zone.";

    private DnsOperationDescriptions() {
    }
}
