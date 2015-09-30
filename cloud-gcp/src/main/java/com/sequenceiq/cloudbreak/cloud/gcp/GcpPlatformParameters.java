package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.domain.CloudRegion;

@Service
public class GcpPlatformParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);

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
        for (DiskType diskType : DiskType.values()) {
            disks.put(diskType.name(), diskType.value);
        }
        return disks;
    }

    @Override
    public String defaultDiskType() {
        return diskTypes().get(DiskType.HDD.name());
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
        return Region.US_CENTRAL1_A.name();
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
        return VmType.N1_STANDARD_2.name();
    }

    private enum DiskType {
        SSD("pd-ssd"), HDD("pd-standard");

        private final String value;

        private DiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public String getUrl(String projectId, Region zone) {
            return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), value);
        }
    }

    private enum VmType {

        N1_STANDARD_1("n1-standard-1"),
        N1_STANDARD_2("n1-standard-2"),
        N1_STANDARD_4("n1-standard-4"),
        N1_STANDARD_8("n1-standard-8"),
        N1_STANDARD_16("n1-standard-16"),
        N1_HIGHMEM_2("n1-highmem-2"),
        N1_HIGHMEM_4("n1-highmem-4"),
        N1_HIGHMEM_8("n1-highmem-8"),
        N1_HIGHMEM_16("n1-highmem-16"),
        N1_HIGHCPU_2("n1-highcpu-2"),
        N1_HIGHCPU_4("n1-highcpu-4"),
        N1_HIGHCPU_8("n1-highcpu-8"),
        N1_HIGHCPU_16("n1-highcpu-16"),
        F1_MICRO("f1-micro"),
        G1_SMALL("g1-small");

        private final String value;

        private VmType(String value) {
            this.value = value;
        }

        public String vmType() {
            return value;
        }
    }

    private enum Region {
        US_CENTRAL1_A("us-central1-a", Arrays.asList("us-central1")),
        US_CENTRAL1_B("us-central1-b", Arrays.asList("us-central1")),
        US_CENTRAL1_F("us-central1-f", Arrays.asList("us-central1")),
        US_CENTRAL1_C("us-central1-c", Arrays.asList("us-central1")),
        EUROPE_WEST1_B("europe-west1-b", Arrays.asList("europe-west1")),
        EUROPE_WEST1_C("europe-west1-c", Arrays.asList("europe-west1")),
        EUROPE_WEST1_D("europe-west1-d", Arrays.asList("europe-west1")),
        ASIA_EAST1_A("asia-east1-a", Arrays.asList("asia-east1")),
        ASIA_EAST1_B("asia-east1-b", Arrays.asList("asia-east1")),
        ASIA_EAST1_C("asia-east1-c", Arrays.asList("asia-east1"));

        private final String value;
        private final List<String> availabilityZones;

        private Region(String value, List<String> availabilityZones) {
            this.value = value;
            this.availabilityZones = availabilityZones;
        }

        public String value() {
            return this.value;
        }

        public List<String> availabilityZones() {
            return this.availabilityZones;
        }

    }
}
