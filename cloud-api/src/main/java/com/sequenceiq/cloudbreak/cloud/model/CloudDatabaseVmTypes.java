package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudDatabaseVmTypes {

    private Map<Region, Set<String>> cloudDatabaseVmResponses = new HashMap<>();

    private Map<Region, String> defaultCloudDatabaseVmResponses = new HashMap<>();

    public CloudDatabaseVmTypes() {
    }

    public CloudDatabaseVmTypes(Map<Region, Set<String>> cloudDatabaseVmResponses, Map<Region, String> defaultCloudDatabaseVmResponses) {
        this.cloudDatabaseVmResponses = cloudDatabaseVmResponses;
        this.defaultCloudDatabaseVmResponses = defaultCloudDatabaseVmResponses;
    }

    public Map<Region, Set<String>> getCloudDatabaseVmResponses() {
        return cloudDatabaseVmResponses;
    }

    public void setCloudDatabaseVmResponses(Map<Region, Set<String>> cloudDatabaseVmResponses) {
        this.cloudDatabaseVmResponses = cloudDatabaseVmResponses;
    }

    public Map<Region, String> getDefaultCloudDatabaseVmResponses() {
        return defaultCloudDatabaseVmResponses;
    }

    public void setDefaultCloudDatabaseVmResponses(Map<Region, String> defaultCloudDatabaseVmResponses) {
        this.defaultCloudDatabaseVmResponses = defaultCloudDatabaseVmResponses;
    }

    @Override
    public String toString() {
        return "CloudDatabaseVmTypes{"
                + "cloudDatabaseVmResponses=" + cloudDatabaseVmResponses
                + ", defaultCloudDatabaseVmResponses=" + defaultCloudDatabaseVmResponses
                + '}';
    }
}
