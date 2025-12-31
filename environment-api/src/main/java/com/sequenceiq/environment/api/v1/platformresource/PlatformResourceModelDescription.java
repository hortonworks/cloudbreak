package com.sequenceiq.environment.api.v1.platformresource;

public class PlatformResourceModelDescription {
    public static final String DISK_TYPES = "disk types";
    public static final String DEFAULT_DISKS = "default disks";
    public static final String DISK_MAPPINGS = "disk mappings";
    public static final String DISK_DISPLAYNAMES = "disk displayNames";
    public static final String REGION_LOCATIONS = "regions with location data";
    public static final String K8S_SUPPORTED_LOCATIONS = "regions with k8s support";
    public static final String CDP_SERVICES = "regions with cdp services support";
    public static final String AVAILABILITY_ZONES = "availability zones";
    public static final String DEFAULT_REGIOS = "default regions";
    public static final String TAG_SPECIFICATIONS = "tag specifications";
    public static final String VIRTUAL_MACHINES = "virtual machines";
    public static final String DEFAULT_VIRTUAL_MACHINES = "default virtual machines";
    public static final String CONNECTOR_V1_DESCRIPTION = "Returns cloud provider specific resource types";

    private PlatformResourceModelDescription() {
    }

    public static class OpDescription {
        public static final String GET_DISK_TYPES = "retrieve available disk types";
        public static final String GET_REGIONS_BY_CREDENTIAL = "retrieve regions by credential";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint";
        public static final String GET_TAG_SPECIFICATIONS = "retrieve tag specifications";
        public static final String GET_NETWORKS = "retrieve network properties";
        public static final String GET_SECURITYGROUPS = "retrieve securitygroups properties";
        public static final String GET_SSHKEYS = "retrieve sshkeys properties";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrieve vmtype properties by credential";
        public static final String GET_DATABASE_VMTYPES_BY_CREDENTIAL = "retrieve database vmtype properties by credential";
        public static final String GET_GATEWAYS = "retrieve gateways with properties";
        public static final String GET_IPPOOLS = "retrieve ip pools with properties";
        public static final String GET_ACCESSCONFIGS = "retrieve access configs with properties";
        public static final String GET_ENCRYPTIONKEYS = "retrieve encryption keys with properties";
        public static final String GET_NOSQL_TABLES = "retrieve nosql tables";
        public static final String GET_RESOURCE_GROUPS = "retrieve resource groups";
        public static final String GET_PRIVATE_DNS_ZONES = "retrieve private DNS zones";
        public static final String GET_REQUIREMENTS = "retrieve requirements";
    }

    public static class OpEnvDescription {
        public static final String GET_DISK_TYPES = "retrieve available disk types by environment";
        public static final String GET_REGIONS_BY_ENVIRONMENT = "retrieve regions by environment";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint by environment";
        public static final String GET_TAG_SPECIFICATIONS = "retrieve tag specifications by environment";
        public static final String GET_NETWORKS = "retrieve network properties by environment";
        public static final String GET_SECURITYGROUPS = "retrieve securitygroups properties by environment";
        public static final String GET_SSHKEYS = "retrieve sshkeys properties by environment";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrieve vmtype properties by environment";
        public static final String GET_VERTICAL_SCALE_RECOMMENDATION = "get vertical scale recommendation";
        public static final String GET_GATEWAYS = "retrieve gateways with properties by environment";
        public static final String GET_IPPOOLS = "retrieve ip pools with properties by environment";
        public static final String GET_ACCESSCONFIGS = "retrieve access configs with properties by environment";
        public static final String GET_ENCRYPTIONKEYS = "retrieve encryption keys with properties by environment";
        public static final String GET_NOSQL_TABLES = "retrieve nosql tables by environment";
        public static final String GET_RESOURCE_GROUPS = "retrieve resource groups by environment";

        public static final String GET_DATABASE_CAPABILITIES = "retrieve database capabilities by environment";
        public static final String GET_PRIVATE_DNS_ZONES = "retrieve private DNS zones by environment";
    }

}
