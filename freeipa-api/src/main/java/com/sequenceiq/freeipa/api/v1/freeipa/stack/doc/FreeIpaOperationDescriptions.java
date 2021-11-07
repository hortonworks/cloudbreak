package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public final class FreeIpaOperationDescriptions {
    public static final String CREATE = "Create FreeIPA stack";
    public static final String REGISTER_CHILD_ENVIRONMENT = "Register a child environment";
    public static final String DEREGISTER_CHILD_ENVIRONMENT = "Deregister a child environment";
    public static final String GET_BY_ENVID = "Get FreeIPA stack by environment CRN";
    public static final String GET_ALL_BY_ENVID = "Get all FreeIPA stacks by environment CRN";
    public static final String INTERNAL_GET_ALL_BY_ENVID_AND_ACCOUNTID = "Get all FreeIPA stacks by environment CRN and account ID using the internal actor";
    public static final String INTERNAL_GET_BY_ENVID_AND_ACCOUNTID = "Get FreeIPA stack by environment CRN and account ID using the internal actor";
    public static final String LIST_BY_ACCOUNT = "List all FreeIPA stacks by account";
    public static final String INTERNAL_LIST_BY_ACCOUNT = "List all FreeIPA stacks by account using the internal actor";
    public static final String GET_ROOTCERTIFICATE_BY_ENVID = "Get FreeIPA root certificate by environment CRN";
    public static final String DELETE_BY_ENVID = "Delete FreeIPA stack by environment CRN";
    public static final String CLEANUP = "Cleans out users, hosts and related DNS entries";
    public static final String INTERNAL_CLEANUP = "Cleans out users, hosts and related DNS entries using internal actor";
    public static final String START = "Start all FreeIPA stacks that attached to the given environment CRN";
    public static final String STOP = "Stop all FreeIPA stacks that attached to the given environment CRN";
    public static final String REGISTER_WITH_CLUSTER_PROXY = "Registers FreeIPA stack with given environment CRN with cluster proxy";
    public static final String DEREGISTER_WITH_CLUSTER_PROXY = "Deregisters FreeIPA stack with given environment CRN with cluster proxy";
    public static final String HEALTH = "Provides a detailed health of the FreeIPA stack";
    public static final String REBOOT = "Reboot one or more instances";
    public static final String REPAIR = "Repair one or more instances";
    public static final String REBUILD = "Rebuild the FreeIPA cluster";
    public static final String BIND_USER_CREATE = "Creates kerberos and ldap bind users for cluster";
    public static final String UPDATE_SALT = "Update salt states on FreeIPA instances";
    public static final String CHANGE_IMAGE = "Changes the image used for creating instances";
    public static final String UPGRADE_FREEIPA = "Upgrades FreeIPA to the latest or defined image";
    public static final String UPGRADE_OPTIONS = "Get available images for FreeIPA upgrade. If catalog is defined use the catalog as image source.";
    public static final String RETRY = "Retries the latest failed operation";
    public static final String LIST_RETRYABLE_FLOWS = "List retryable failed flows";
    public static final String CHANGE_IMAGE_CATALOG = "Changes the image catalog used for creating instances";
    public static final String GENERATE_IMAGE_CATALOG = "Generates an image catalog that only contains the currently used image for creating instances";
    public static final String INTERNAL_UPGRADE_CCM_BY_ENVID =
            "Initiates the CCM tunnel type upgrade to the latest available version for FreeIPA stack by environment CRN using the internal actor";

    private FreeIpaOperationDescriptions() {
    }
}
