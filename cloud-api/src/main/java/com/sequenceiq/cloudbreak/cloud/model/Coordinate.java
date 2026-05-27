package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.common.model.Architecture.ARM64;
import static com.sequenceiq.common.model.Architecture.X86_64;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;

public class Coordinate {

    private final String displayName;

    private final Double longitude;

    private final Double latitude;

    private final String key;

    private final boolean k8sSupported;

    private final DefaultVmTypes defaultVmtypes;

    private final List<String> entitlements;

    private final Set<CdpSupportedServices> cdpSupportedServices;

    protected Coordinate(
            Double longitude,
            Double latitude,
            String displayName,
            String key,
            boolean k8sSupported,
            List<String> entitlements,
            DefaultVmTypes defaultVmtypes,
            Set<CdpSupportedServices> cdpSupportedServices) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.displayName = displayName;
        this.key = key;
        this.k8sSupported = k8sSupported;
        this.entitlements = entitlements;
        this.defaultVmtypes = defaultVmtypes != null ? defaultVmtypes : new DefaultVmTypes();
        this.cdpSupportedServices = cdpSupportedServices;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean getK8sSupported() {
        return k8sSupported;
    }

    public String getKey() {
        return key;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public boolean isK8sSupported() {
        return k8sSupported;
    }

    public DefaultVmTypes getDefaultVmtypes() {
        return defaultVmtypes;
    }

    public List<String> getDefaultDbVmTypes() {
        return defaultVmtypes.getDatabaseVmType(X86_64);
    }

    public List<String> getDefaultArmDbVmTypes() {
        return defaultVmtypes.getDatabaseVmType(ARM64);
    }

    public List<String> getDefaultX86FreeIPAVmtypes() {
        return defaultVmtypes.getFreeipaVmType(X86_64);
    }

    public List<String> getDefaultArmFreeIPAVmtypes() {
        return defaultVmtypes.getFreeipaVmType(ARM64);
    }

    public Set<CdpSupportedServices> getCdpSupportedServices() {
        return cdpSupportedServices;
    }

    public boolean isMatchedRegion(Region region) {
        return Objects.equals(key, region.getRegionName()) || Objects.equals(displayName, region.getRegionName());
    }

    public static Coordinate coordinate(
            String longitude,
            String latitude,
            String displayName,
            String key,
            boolean k8sSupported,
            List<String> entitlements,
            DefaultVmTypes defaultVmtypes,
            Set<CdpSupportedServices> cdpSupportedServices) {
        return new Coordinate(
                Double.parseDouble(longitude),
                Double.parseDouble(latitude),
                displayName,
                key,
                k8sSupported,
                entitlements,
                defaultVmtypes,
                cdpSupportedServices == null ? new HashSet<>() : cdpSupportedServices);
    }

    public static Coordinate defaultCoordinate() {
        return new Coordinate(
                Double.parseDouble("36.7477169"),
                Double.parseDouble("-119.7729841"),
                "California (West US)",
                "us-west-1",
                false,
                new ArrayList<>(),
                new DefaultVmTypes(),
                Set.of());
    }
}
