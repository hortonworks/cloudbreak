package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudVmTypes {

    private Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();

    private Map<String, Set<VmType>> deprecatedCloudVmResponses = new HashMap<>();

    private Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

    public CloudVmTypes() {
    }

    public CloudVmTypes(Map<String, Set<VmType>> cloudVmResponses, Map<String, VmType> defaultCloudVmResponses) {
        this(cloudVmResponses, new HashMap<>(), defaultCloudVmResponses);
    }

    public CloudVmTypes(Map<String, Set<VmType>> cloudVmResponses, Map<String, Set<VmType>> deprecatedCloudVmResponses,
            Map<String, VmType> defaultCloudVmResponses) {
        this.cloudVmResponses = cloudVmResponses;
        this.deprecatedCloudVmResponses = deprecatedCloudVmResponses;
        this.defaultCloudVmResponses = defaultCloudVmResponses;
    }

    public Map<String, Set<VmType>> getCloudVmResponses() {
        return cloudVmResponses;
    }

    public void setCloudVmResponses(Map<String, Set<VmType>> cloudVmResponses) {
        this.cloudVmResponses = cloudVmResponses;
    }

    public Map<String, Set<VmType>> getDeprecatedCloudVmResponses() {
        return deprecatedCloudVmResponses;
    }

    public void setDeprecatedCloudVmResponses(Map<String, Set<VmType>> deprecatedCloudVmResponses) {
        this.deprecatedCloudVmResponses = deprecatedCloudVmResponses;
    }

    public Map<String, VmType> getDefaultCloudVmResponses() {
        return defaultCloudVmResponses;
    }

    public void setDefaultCloudVmResponses(Map<String, VmType> defaultCloudVmResponses) {
        this.defaultCloudVmResponses = defaultCloudVmResponses;
    }

    @Override
    public String toString() {
        return "CloudVmTypes{" +
                "cloudVmResponses=" + cloudVmResponses +
                ", deprecatedCloudVmResponses=" + deprecatedCloudVmResponses +
                ", defaultCloudVmResponses=" + defaultCloudVmResponses +
                '}';
    }
}
