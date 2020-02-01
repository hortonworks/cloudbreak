package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public final class FreeIpaOperationDescriptions {
    public static final String CREATE = "Create FreeIpa stack";
    public static final String GET_BY_ENVID = "Get FreeIPA stack by envid";
    public static final String LIST_BY_ACCOUNT = "List all FreeIPA stacks by account";
    public static final String GET_ROOTCERTIFICATE_BY_ENVID = "Get FreeIPA root certificate by envid";
    public static final String DELETE_BY_ENVID = "Delete FreeIPA stack by envid";
    public static final String CLEANUP = "Cleans out users, hosts and related DNS entries";
    public static final String START = "Start all FreeIPA stacks that attached to the given environment CRN";
    public static final String STOP = "Stop all FreeIPA stacks that attached to the given environment CRN";
    public static final String REGISTER_WITH_CLUSTER_PROXY = "Registers FreeIPA stack with given environment CRN with cluster proxy";
    public static final String DEREGISTER_WITH_CLUSTER_PROXY = "Deregisters FreeIPA stack with given environment CRN with cluster proxy";
    public static final String HEALTH = "Provides a detailed health of the FreeIPA stack";

    private FreeIpaOperationDescriptions() {
    }
}
