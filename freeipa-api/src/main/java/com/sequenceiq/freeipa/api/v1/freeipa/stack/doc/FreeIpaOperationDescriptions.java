package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public final class FreeIpaOperationDescriptions {
    public static final String CREATE = "Create FreeIpa stack";
    public static final String GET_BY_ENVID = "Get FreeIPA stack by envid";
    public static final String LIST_BY_ACCOUNT = "List all FreeIPA stacks by account";
    public static final String GET_ROOTCERTIFICATE_BY_ENVID = "Get FreeIPA root certificate by envid";
    public static final String DELETE_BY_ENVID = "Delete FreeIPA stack by envid";
    public static final String ADD_DNS_ZONE_FOR_SUBNETS = "Creates reverse DNS Zone entry for subnets in CIDR format";
    public static final String ADD_DNS_ZONE_FOR_SUBNET_IDS = "Creates reverse DNS Zone entry for subnet IDs";
    public static final String LIST_DNS_ZONES = "List DNS zones available in FreeIPA";
    public static final String DELETE_DNS_ZONE_BY_SUBNET = "Deletes reverse DNS Zone entry by subnet CIDR";
    public static final String DELETE_DNS_ZONE_BY_SUBNET_ID = "Deletes reverse DNS Zone entry by subnet ID";

    private FreeIpaOperationDescriptions() {
    }
}
