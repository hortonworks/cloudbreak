package com.sequenceiq.environment.api.v1.platformresource;

public class PlatformResourceModelDescription {
    public static final String DISK_TYPES = "disk types";
    public static final String DEFAULT_DISKS = "default disks";
    public static final String DISK_MAPPINGS = "disk mappings";
    public static final String DISK_DISPLAYNAMES = "disk displayNames";
    public static final String REGIONS = "regions";
    public static final String REGION_DISPLAYNAMES = "regions with displayNames";
    public static final String REGION_LOCATIONS = "regions with location data";
    public static final String K8S_SUPPORTED_LOCATIONS = "regions with k8s support";
    public static final String AVAILABILITY_ZONES = "availability zones";
    public static final String DEFAULT_REGIOS = "default regions";
    public static final String TAG_SPECIFICATIONS = "tag specifications";
    public static final String VIRTUAL_MACHNES = "virtual machines";
    public static final String DEFAULT_VIRTUAL_MACHINES = "default virtual machines";
    public static final String CONNECTOR_V1_DESCRIPTION = "Returns cloud provider specific resource types by workspace";

    public static class OpDescription {
        public static final String GET_DISK_TYPES = "retrieve available disk types";
        public static final String GET_REGION_R_BY_TYPE = "retrieve regions by type";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint";
        public static final String GET_TAG_SPECIFICATIONS = "retrieve tag specifications";
        public static final String GET_NETWORKS = "retrieve network properties";
        public static final String GET_SECURITYGROUPS = "retrieve securitygroups properties";
        public static final String GET_SSHKEYS = "retrieve sshkeys properties";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrieve vmtype properties by credential";
        public static final String GET_GATEWAYS = "retrieve gateways with properties";
        public static final String GET_IPPOOLS = "retrieve ip pools with properties";
        public static final String GET_ACCESSCONFIGS = "retrieve access configs with properties";
        public static final String GET_ENCRYPTIONKEYS = "retrieve encryption keys with properties";
        public static final String GET_NOSQL_TABLES = "retrieve nosql tables";
    }

    private PlatformResourceModelDescription() {
    }
}
