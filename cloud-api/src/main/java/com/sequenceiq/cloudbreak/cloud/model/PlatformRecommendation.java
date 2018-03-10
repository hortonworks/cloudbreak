package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PlatformRecommendation {

    private Map<String, VmType> recommendations;

    private Set<VmType> virtualMachines;

    private DiskTypes diskTypes;

    public PlatformRecommendation(Map<String, VmType> recommendations, Set<VmType> virtualMachines, DiskTypes diskTypes) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.diskTypes = diskTypes;
    }

    public Map<String, VmType> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Map<String, VmType> recommendations) {
        this.recommendations = recommendations;
    }

    public Collection<VmType> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmType> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public DiskTypes getDiskTypes() {
        return diskTypes;
    }

    public void setDiskTypes(DiskTypes diskTypes) {
        this.diskTypes = diskTypes;
    }
}
