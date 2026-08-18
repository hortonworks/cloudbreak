package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Set;

public class PlatformRecommendation {

    private Map<String, VmType> recommendations;

    private Set<VmType> virtualMachines;

    private Set<VmType> deprecatedVirtualMachines;

    private DiskTypes diskTypes;

    private Map<String, InstanceCount> instanceCounts;

    private GatewayRecommendation gatewayRecommendation;

    private AutoscaleRecommendation autoscaleRecommendation;

    private ResizeRecommendation resizeRecommendation;

    public PlatformRecommendation(
            Map<String, VmType> recommendations,
            Set<VmType> virtualMachines,
            Set<VmType> deprecatedVirtualMachines,
            DiskTypes diskTypes,
            Map<String, InstanceCount> instanceCounts,
            GatewayRecommendation gatewayRecommendation,
            AutoscaleRecommendation autoscaleRecommendation,
            ResizeRecommendation resizeRecommendation
    ) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.deprecatedVirtualMachines = deprecatedVirtualMachines;
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

    public Set<VmType> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmType> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public Set<VmType> getDeprecatedVirtualMachines() {
        return deprecatedVirtualMachines;
    }

    public void setDeprecatedVirtualMachines(Set<VmType> deprecatedVirtualMachines) {
        this.deprecatedVirtualMachines = deprecatedVirtualMachines;
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

    public void setInstanceCounts(Map<String, InstanceCount> instanceCounts) {
        this.instanceCounts = instanceCounts;
    }

    public GatewayRecommendation getGatewayRecommendation() {
        return gatewayRecommendation;
    }

    public void setGatewayRecommendation(GatewayRecommendation gatewayRecommendation) {
        this.gatewayRecommendation = gatewayRecommendation;
    }

    public AutoscaleRecommendation getAutoscaleRecommendation() {
        return autoscaleRecommendation;
    }

    public void setAutoscaleRecommendation(AutoscaleRecommendation autoscaleRecommendation) {
        this.autoscaleRecommendation = autoscaleRecommendation;
    }

    public ResizeRecommendation getResizeRecommendation() {
        return resizeRecommendation;
    }

    public void setResizeRecommendation(ResizeRecommendation resizeRecommendation) {
        this.resizeRecommendation = resizeRecommendation;
    }

    @Override
    public String toString() {
        return "PlatformRecommendation{" +
                "recommendations=" + recommendations +
                ", virtualMachines=" + virtualMachines +
                ", deprecatedVirtualMachines=" + deprecatedVirtualMachines +
                ", diskTypes=" + diskTypes +
                ", instanceCounts=" + instanceCounts +
                ", gatewayRecommendation=" + gatewayRecommendation +
                ", autoscaleRecommendation=" + autoscaleRecommendation +
                ", resizeRecommendation=" + resizeRecommendation +
                '}';
    }
}
