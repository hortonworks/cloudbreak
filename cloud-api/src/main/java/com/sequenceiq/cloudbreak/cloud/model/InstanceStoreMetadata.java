package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class InstanceStoreMetadata {

    private Map<String, Integer> instaceStoreCountMap = new HashMap<>();

    public InstanceStoreMetadata() {
    }

    public InstanceStoreMetadata(Map<String, Integer> instaceStoreCountMap) {
        this.instaceStoreCountMap = instaceStoreCountMap;
    }

    public void addInstanceStoreCountToInstanceType(String instanceType, Integer instanceStoreCount) {
        instaceStoreCountMap.put(instanceType, instanceStoreCount);
    }

    public Integer mapInstanceTypeToInstanceStoreCount(String instanceType) {
        return instaceStoreCountMap.get(instanceType);
    }

    public Integer mapInstanceTypeToInstanceStoreCountNullHandled(String instanceType) {
        Integer instanceStoreCount = instaceStoreCountMap.get(instanceType);
        return instanceStoreCount != null ? instanceStoreCount : 0;
    }
}
