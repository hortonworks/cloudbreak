package com.sequenceiq.environment.api.platformresource;

public class PlatformResourceModelDescription {
    public static final String DISK_TYPES = "disk types";
    public static final String DEFAULT_DISKS = "default disks";
    public static final String DISK_MAPPINGS = "disk mappings";
    public static final String DISK_DISPLAYNAMES = "disk displayNames";
    public static final String REGIONS = "regions";
    public static final String REGION_DISPLAYNAMES = "regions with displayNames";
    public static final String REGION_LOCATIONS = "regions with location data";
    public static final String AVAILABILITY_ZONES = "availability zones";
    public static final String DEFAULT_REGIOS = "default regions";
    public static final String TAG_SPECIFICATIONS = "tag specifications";
    public static final String VIRTUAL_MACHNES = "virtual machines";
    public static final String DEFAULT_VIRTUAL_MACHINES = "default virtual machines";
    public static final String CONNECTOR_V1_DESCRIPTION = "Returns cloud provider specific resource types by workspace";
    public static final String JSON_CONTENT_TYPE = "application/json";

    public static class OpDescription {
        public static final String GET_DISK_TYPES = "retrive available disk types";
        public static final String GET_REGION_R_BY_TYPE = "retrive regions by type";
        public static final String GET_RECOMMENDATION = "creates a recommendation that advises cloud resources for the given blueprint";
        public static final String GET_TAG_SPECIFICATIONS = "retrive tag specifications";
        public static final String GET_NETWORKS = "retrive network properties";
        public static final String GET_SECURITYGROUPS = "retrive securitygroups properties";
        public static final String GET_SSHKEYS = "retrive sshkeys properties";
        public static final String GET_VMTYPES_BY_CREDENTIAL = "retrive vmtype properties by credential";
        public static final String GET_GATEWAYS = "retrive gateways with properties";
        public static final String GET_IPPOOLS = "retrive ip pools with properties";
        public static final String GET_ACCESSCONFIGS = "retrive access configs with properties";
        public static final String GET_ENCRYPTIONKEYS = "retrive encryption keys with properties";
    }

    private PlatformResourceModelDescription() {
    }
}
