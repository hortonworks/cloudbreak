package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PlatformRecommendation {

    private Map<String, VmType> recommendations;

    private Set<VmType> virtualMachines;

    private DiskTypes diskTypes;

    private Map<String, InstanceCount> instanceCounts;

    private GatewayRecommendation gatewayRecommendation;

    private AutoscaleRecommendation autoscaleRecommendation;

    private ResizeRecommendation resizeRecommendation;

    public PlatformRecommendation(
            Map<String, VmType> recommendations,
            Set<VmType> virtualMachines,
            DiskTypes diskTypes,
            Map<String, InstanceCount> instanceCounts,
            GatewayRecommendation gatewayRecommendation,
            AutoscaleRecommendation autoscaleRecommendation,
            ResizeRecommendation resizeRecommendation
    ) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.diskTypes = diskTypes;
        this.instanceCounts = instanceCounts;
        this.gatewayRecommendation = gatewayRecommendation;
        this.autoscaleRecommendation = autoscaleRecommendation;
        this.resizeRecommendation = resizeRecommendation;
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

    public Map<String, InstanceCount> getInstanceCounts() {
        return instanceCounts;
    }

    public GatewayRecommendation getGatewayRecommendation() {
        return gatewayRecommendation;
    }

    public AutoscaleRecommendation getAutoscaleRecommendation() {
        return autoscaleRecommendation;
    }

    public ResizeRecommendation getResizeRecommendation() {
        return resizeRecommendation;
    }
}
