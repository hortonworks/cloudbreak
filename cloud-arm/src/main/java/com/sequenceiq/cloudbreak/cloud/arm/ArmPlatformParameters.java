package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class ArmPlatformParameters implements PlatformParameters {

    private static final int START_LABEL = 98;

    @Override
    public String diskPrefix() {
        return "sd";
    }

    @Override
    public Integer startLabel() {
        return START_LABEL;
    }

    @Override
    public Map<String, String> diskTypes() {
        Map<String, String> disks = new HashMap<>();
        disks.put("HDD", "HDD");
        return disks;
    }

    @Override
    public String defaultDiskType() {
        return diskTypes().get("HDD");
    }

    @Override
    public Map<String, String> regions() {
        Map<String, String> regions = new HashMap<>();
        for (Region region : Region.values()) {
            regions.put(region.name(), region.value());
        }
        return regions;
    }

    @Override
    public String defaultRegion() {
        return Region.CENTRAL_US.name();
    }

    @Override
    public Map<String, List<String>> availabiltyZones() {
        Map<String, List<String>> regions = new HashMap<>();
        for (Region region : Region.values()) {
            regions.put(region.name(), new ArrayList<String>());
        }
        return regions;
    }

    @Override
    public Map<String, String> virtualMachines() {
        Map<String, String> vmTypes = new HashMap<>();
        for (VmType vmType : VmType.values()) {
            vmTypes.put(vmType.name(), vmType.vmType());
        }
        return vmTypes;
    }

    @Override
    public String defaultVirtualMachine() {
        return VmType.STANDARD_D3.name();
    }

    private enum VmType {

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

        private VmType(String vmType, int maxDiskSize) {
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

    private enum Region {
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

        private Region(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

    }
}
