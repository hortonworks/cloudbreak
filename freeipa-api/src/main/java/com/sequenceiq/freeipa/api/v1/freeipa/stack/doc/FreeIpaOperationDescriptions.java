package com.sequenceiq.freeipa.api.v1.freeipa.stack.doc;

public final class FreeIpaOperationDescriptions {
    public static final String CREATE = "Create FreeIPA stack";

    public static final String COST = "Show FreeIPA cost";
    public static final String CO2 = "Show FreeIPA CO2 cost";
    public static final String REGISTER_CHILD_ENVIRONMENT = "Register a child environment";
    public static final String DEREGISTER_CHILD_ENVIRONMENT = "Deregister a child environment";
    public static final String GET_BY_ENVID = "Get FreeIPA stack by environment CRN";
    public static final String GET_ALL_BY_ENVID = "Get all FreeIPA stacks by environment CRN";
    public static final String INTERNAL_GET_ALL_BY_ENVID_AND_ACCOUNTID = "Get all FreeIPA stacks by environment CRN and account ID using the internal actor";
    public static final String INTERNAL_GET_BY_ENVID_AND_ACCOUNTID = "Get FreeIPA stack by environment CRN and account ID using the internal actor";
    public static final String LIST_BY_ACCOUNT = "List all FreeIPA stacks by account";
    public static final String INTERNAL_LIST_BY_ACCOUNT = "List all FreeIPA stacks by account using the internal actor";
    public static final String GET_ROOTCERTIFICATE_BY_ENVID = "Get FreeIPA root certificate by environment CRN";
    public static final String INTERNAL_GET_ROOTCERTIFICATE_BY_ENVID_AND_ACCOUNTID =
            "Get FreeIPA root certificate by environment CRN and account ID using the internal actor";
    public static final String DELETE_BY_ENVID = "Delete FreeIPA stack by environment CRN";
    public static final String CLEANUP = "Cleans out users, hosts and related DNS entries";
    public static final String INTERNAL_CLEANUP = "Cleans out users, hosts and related DNS entries using internal actor";
    public static final String START = "Start all FreeIPA stacks that attached to the given environment CRN";
    public static final String STOP = "Stop all FreeIPA stacks that attached to the given environment CRN";
    public static final String ROTATE_SALT_PASSWORD = "Rotate SaltStack user password of FreeIPA stacks that attached to the given environment CRN";
    public static final String REGISTER_WITH_CLUSTER_PROXY = "Registers FreeIPA stack with given environment CRN with cluster proxy";
    public static final String DEREGISTER_WITH_CLUSTER_PROXY = "Deregisters FreeIPA stack with given environment CRN with cluster proxy";
    public static final String HEALTH = "Provides a detailed health of the FreeIPA stack";
    public static final String REBOOT = "Reboot one or more instances";
    public static final String REPAIR = "Repair one or more instances";
    public static final String REBUILD = "Rebuild the FreeIPA cluster";
    public static final String CANCEL_CROSS_REALM_TRUST = "Cancel cross-realm trust of FreeIPA with an Active Directory Server";
    public static final String REPAIR_CROSS_REALM_TRUST = "Repair cross-realm trust of FreeIPA with an Active Directory Server";
    public static final String SETUP_CROSS_REALM_TRUST = "Prepare cross-realm trust of FreeIPA with an Active Directory Server";
    public static final String SETUP_FINISH_CROSS_REALM_TRUST = "Finish setting up cross-realm trust of FreeIPA with an Active Directory Server";
    public static final String TRUST_SETUP_COMMANDS = "Get commands to be run for cross-realm trust setup";
    public static final String TRUST_CLEANUP_COMMANDS = "Get commands to be run for cross-realm trust cleanup after cancel";
    public static final String BIND_USER_CREATE = "Creates kerberos and ldap bind users for cluster";
    public static final String UPDATE_SALT = "Update salt states on FreeIPA instances";
    public static final String CHANGE_IMAGE = "Changes the image used for creating instances";
    public static final String GET_IMAGE = "Get current image used for creating instances";
    public static final String UPGRADE_FREEIPA = "Upgrades FreeIPA to the latest or defined image";
    public static final String UPGRADE_OPTIONS = "Get available images for FreeIPA upgrade. If catalog is defined use the catalog as image source.";
    public static final String UPSCALE_FREEIPA = "Upscales FreeIPA instances";
    public static final String DOWNSCALE_FREEIPA = "Downscales FreeIPA instances, either by providing a target AvailabilityType, or specifying a list of " +
            "nodes to delete";
    public static final String RETRY = "Retries the latest failed operation";
    public static final String LIST_RETRYABLE_FLOWS = "List retryable failed flows";
    public static final String CHANGE_IMAGE_CATALOG = "Changes the image catalog used for creating instances";
    public static final String GENERATE_IMAGE_CATALOG = "Generates an image catalog that only contains the currently used image for creating instances";
    public static final String INTERNAL_UPGRADE_CCM_BY_ENVID =
            "Initiates the CCM tunnel type upgrade to the latest available version for FreeIPA stack by environment CRN using the internal actor";
    public static final String INTERNAL_MODIFY_PROXY_BY_ENV_ID =
            "Initiates the modification of the proxy config for FreeIPA stack by environment CRN using the internal actor";
    public static final String INTERNAL_GET_OUTBOUND_TYPE =
            "Get the outbound type of the FreeIPA stack by environment CRN using the internal actor";
    public static final String GET_RECOMMENDATION = "Get recommendation that advises cloud resources for FreeIPA based on the given credential CRN.";
    public static final String VERTICAL_SCALE_BY_CRN = "Vertical scale by environment CRN.";
    public static final String ROOT_VOLUME_UPDATE_BY_CRN = "Root Volume update by environment CRN.";
    public static final String IMD_UPDATE = "Stacks' instances metadata update.";
    public static final String ROTATE_SECRETS_BY_CRN = "Rotate secrets by environment CRN.";
    public static final String GET_USED_SUBNETS_BY_ENVIRONMENT_CRN = "List the used subnets by the given Environment resource CRN";

    public static final String GET_ENCRYPTION_KEYS = "Get encryption keys of the FreeIPA";

    public static final String MODIFY_SELINUX_BY_CRN = "Modifies SELinux of the FreeIPA instances";

    private FreeIpaOperationDescriptions() {
    }
}
