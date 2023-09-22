package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class InstanceTypeMetadata {

    private Map<String, String> instanceTypes;

    public InstanceTypeMetadata() {
        this.instanceTypes = new HashMap<>();
    }

    public InstanceTypeMetadata(Map<String, String> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    public Map<String, String> getInstanceTypes() {
        return instanceTypes;
    }

    public void setInstanceTypes(Map<String, String> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    @Override
    public String toString() {
        return "InstanceTypeMetadata{" +
                "instanceTypes=" + instanceTypes +
                '}';
    }
}
