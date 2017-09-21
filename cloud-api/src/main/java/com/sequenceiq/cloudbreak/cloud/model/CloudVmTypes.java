package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudVmTypes {

    private Map<String, Set<VmType>> cloudVmResponses = new HashMap<>();

    private Map<String, VmType> defaultCloudVmResponses = new HashMap<>();

    public CloudVmTypes() {
    }

    public CloudVmTypes(Map<String, Set<VmType>> cloudVmResponses, Map<String, VmType> defaultCloudVmResponses) {
        this.cloudVmResponses = cloudVmResponses;
        this.defaultCloudVmResponses = defaultCloudVmResponses;
    }

    public Map<String, Set<VmType>> getCloudVmResponses() {
        return cloudVmResponses;
    }

    public void setCloudVmResponses(Map<String, Set<VmType>> cloudVmResponses) {
        this.cloudVmResponses = cloudVmResponses;
    }

    public Map<String, VmType> getDefaultCloudVmResponses() {
        return defaultCloudVmResponses;
    }

    public void setDefaultCloudVmResponses(Map<String, VmType> defaultCloudVmResponses) {
        this.defaultCloudVmResponses = defaultCloudVmResponses;
    }
}
