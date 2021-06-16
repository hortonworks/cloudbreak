package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InstanceStoreMetadata {

    private Map<String, VolumeParameterConfig> instaceStoreConfigMap = new HashMap<>();

    public InstanceStoreMetadata() {
    }

    public InstanceStoreMetadata(Map<String, VolumeParameterConfig> instaceStoreConfigMap) {
        this.instaceStoreConfigMap = Optional.ofNullable(instaceStoreConfigMap).orElse(new HashMap<>());
    }

    public void addInstanceStoreConfigToInstanceType(String instanceType, VolumeParameterConfig instanceStoreConfig) {
        instaceStoreConfigMap.put(instanceType, instanceStoreConfig);
    }

    public Integer mapInstanceTypeToInstanceStoreCount(String instanceType) {
        return instaceStoreConfigMap.getOrDefault(instanceType, VolumeParameterConfig.EMPTY).maximumNumber();
    }

    public Integer mapInstanceTypeToInstanceStoreCountNullHandled(String instanceType) {
        Integer instanceStoreCount = instaceStoreConfigMap.getOrDefault(instanceType, VolumeParameterConfig.EMPTY).maximumNumber();
        return instanceStoreCount != null ? instanceStoreCount : 0;
    }

    public Map<String, VolumeParameterConfig> getInstaceStoreConfigMap() {
        return instaceStoreConfigMap;
    }
}
