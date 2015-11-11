package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;

@Service
public class ArmPlatformParameters implements PlatformParameters {

    private static final int START_LABEL = 98;
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType());

    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        disks.add(diskType("HDD"));
        return disks;
    }

    private DiskType defaultDiskType() {
        return diskType("HDD");
    }

    @Override
    public Regions regions() {
        return new Regions(getRegions(), defaultRegion());
    }

    private Collection<Region> getRegions() {
        Collection<Region> regions = Lists.newArrayList();
        for (ArmRegion region : ArmRegion.values()) {
            regions.add(region(region.value()));
        }
        return regions;
    }

    private Region defaultRegion() {
        return region(ArmRegion.CENTRAL_US.value());
    }

    @Override
    public AvailabilityZones availabilityZones() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        for (ArmRegion region : ArmRegion.values()) {
            regions.put(region(region.value()), new ArrayList<AvailabilityZone>());
        }
        return new AvailabilityZones(regions);
    }

    @Override
    public VmTypes vmTypes() {
        return new VmTypes(virtualMachines(), defaultVirtualMachine());
    }

    private Collection<VmType> virtualMachines() {
        Collection<VmType> vmTypes = Lists.newArrayList();
        for (ArmVmType vmType : ArmVmType.values()) {
            vmTypes.add(vmType(vmType.vmType()));
        }
        return vmTypes;
    }

    private VmType defaultVirtualMachine() {
        return vmType(ArmVmType.STANDARD_D3.vmType());
    }

    private enum ArmVmType {

        A5("A5", 4),
        A6("A6", 4),
        A7("A7", 8),
        A8("A8", 8),
        A9("A9", 16),
        BASIC_A0("Basic_A0", 1),
        BASIC_A1("Basic_A1", 2),
        BASIC_A2("Basic_A2", 4),
        BASIC_A3("Basic_A3", 8),
        BASIC_A4("Basic_A4", 16),
        STANDARD_G1("Standard_G1", 1),
        STANDARD_G2("Standard_G2", 2),
        STANDARD_G3("Standard_G3", 4),
        STANDARD_G4("Standard_G4", 8),
        STANDARD_G5("Standard_G5", 16),
        STANDARD_D1("Standard_D1", 2),
        STANDARD_D2("Standard_D2", 4),
        STANDARD_D3("Standard_D3", 8),
        STANDARD_D4("Standard_D4", 16),
        STANDARD_D11("Standard_D11", 4),
        STANDARD_D12("Standard_D12", 8),
        STANDARD_D13("Standard_D13", 16),
        STANDARD_D14("Standard_D14", 32);

        private final String vmType;
        private final int maxDiskSize;

        private ArmVmType(String vmType, int maxDiskSize) {
            this.vmType = vmType;
            this.maxDiskSize = maxDiskSize;
        }

        public String vmType() {
            return this.vmType;
        }

        public int maxDiskSize() {
            return this.maxDiskSize;
        }
    }

    private enum ArmRegion {
        EAST_ASIA("East Asia"),
        NORTH_EUROPE("North Europe"),
        WEST_EUROPE("West Europe"),
        EAST_US("East US"),
        CENTRAL_US("Central US"),
        SOUTH_CENTRAL_US("South Central US"),
        NORTH_CENTRAL_US("North Central US"),
        EAST_US_2("East US 2"),
        WEST_US("West US"),
        JAPAN_EAST("Japan East"),
        JAPAN_WEST("Japan West"),
        SOUTHEAST_ASIA("Southeast Asia"),
        BRAZIL_SOUTH("Brazil South");

        private final String value;

        private ArmRegion(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

    }
}
