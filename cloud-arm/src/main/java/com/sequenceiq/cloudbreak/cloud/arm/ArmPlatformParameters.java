package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.meta;

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
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
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
        return Lists.newArrayList();
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
            vmTypes.add(vmTypeWithMeta(vmType.vmType(), vmType.getMeta()));
        }
        return vmTypes;
    }

    private VmType defaultVirtualMachine() {
        return vmType(ArmVmType.STANDARD_D3.vmType());
    }

    public enum ArmVmType {
        STANDARD_A5("Standard_A5", meta(4)),
        STANDARD_A6("Standard_A6", meta(8)),
        STANDARD_A7("Standard_A7", meta(16)),
        STANDARD_A8("Standard_A8", meta(16)),
        STANDARD_A9("Standard_A9", meta(16)),
        STANDARD_A10("Standard_A10", meta(16)),
        STANDARD_A11("Standard_A11", meta(16)),
        STANDARD_G1("Standard_G1", meta(1)),
        STANDARD_G2("Standard_G2", meta(2)),
        STANDARD_G3("Standard_G3", meta(4)),
        STANDARD_G4("Standard_G4", meta(8)),
        STANDARD_G5("Standard_G5", meta(16)),
        STANDARD_D1("Standard_D1", meta(2)),
        STANDARD_D2("Standard_D2", meta(4)),
        STANDARD_D3("Standard_D3", meta(8)),
        STANDARD_D4("Standard_D4", meta(16)),
        STANDARD_D11("Standard_D11", meta(4)),
        STANDARD_D12("Standard_D12", meta(8)),
        STANDARD_D13("Standard_D13", meta(16)),
        STANDARD_D14("Standard_D14", meta(32)),
        STANDARD_D1_V2("Standard_D1_v2", meta(2)),
        STANDARD_D2_V2("Standard_D2_v2", meta(4)),
        STANDARD_D3_V2("Standard_D3_v2", meta(8)),
        STANDARD_D4_V2("Standard_D4_v2", meta(16)),
        STANDARD_D5_V2("Standard_D5_v2", meta(32)),
        STANDARD_D11_V2("Standard_D11_v2", meta(4)),
        STANDARD_D12_V2("Standard_D12_v2", meta(8)),
        STANDARD_D13_V2("Standard_D13_v2", meta(16)),
        STANDARD_D14_V2("Standard_D14_v2", meta(32));

        private final String vmType;
        private final VmTypeMeta meta;

        private ArmVmType(String vmType, VmTypeMeta meta) {
            this.vmType = vmType;
            this.meta = meta;
        }

        public static int getVolumeCountByType(String value) {
            for (ArmVmType item : ArmVmType.values()) {
                if (item.vmType.equals(value)) {
                    return item.getMeta().maxEphemeralVolumeCount();
                }
            }
            throw new IllegalArgumentException(String.format("There's no '%s' azure volume type.", value));
        }

        public String vmType() {
            return this.vmType;
        }

        public VmTypeMeta getMeta() {
            return meta;
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
