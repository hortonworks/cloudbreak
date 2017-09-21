package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlatformRecommendation {

    private Map<String, VmType> recommendations = new HashMap<>();

    private Set<VmType> virtualMachines = new HashSet<>();

    public PlatformRecommendation(Map<String, VmType> recommendations, Set<VmType> virtualMachines) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
    }

    public Map<String, VmType> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Map<String, VmType> recommendations) {
        this.recommendations = recommendations;
    }

    public Set<VmType> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmType> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
