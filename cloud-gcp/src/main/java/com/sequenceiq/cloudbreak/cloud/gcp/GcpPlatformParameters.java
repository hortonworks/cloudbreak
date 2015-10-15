package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
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
public class GcpPlatformParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);
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
        for (GcpDiskType diskType : GcpDiskType.values()) {
            disks.add(DiskType.diskType(diskType.value()));
        }
        return disks;
    }

    private DiskType defaultDiskType() {
        return DiskType.diskType(GcpDiskType.HDD.value());
    }

    @Override
    public Regions regions() {
        return new Regions(getRegions(), defaultRegion());
    }

    private Collection<Region> getRegions() {
        Collection<Region> regions = Lists.newArrayList();
        for (GcpRegion region : GcpRegion.values()) {
            regions.add(Region.region(region.value()));
        }
        return regions;
    }

    private Region defaultRegion() {
        return Region.region(GcpRegion.US_CENTRAL1_A.value());
    }

    @Override
    public AvailabilityZones availabilityZones() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        for (GcpRegion region : GcpRegion.values()) {
            regions.put(Region.region(region.value()), new ArrayList<AvailabilityZone>());
        }
        return new AvailabilityZones(regions);
    }

    @Override
    public VmTypes vmTypes() {
        return new VmTypes(virtualMachines(), defaultVirtualMachine());
    }

    private Collection<VmType> virtualMachines() {
        Collection<VmType> vmTypes = Lists.newArrayList();
        for (GcpVmType vmType : GcpVmType.values()) {
            vmTypes.add(VmType.vmType(vmType.value));
        }
        return vmTypes;
    }

    private VmType defaultVirtualMachine() {
        return VmType.vmType(GcpVmType.N1_STANDARD_2.value);
    }

    private enum GcpDiskType {
        SSD("pd-ssd"), HDD("pd-standard");

        private final String value;

        private GcpDiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public String getUrl(String projectId, GcpRegion zone) {
            return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), value);
        }
    }

    private enum GcpVmType {

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

        private GcpVmType(String value) {
            this.value = value;
        }

        public String vmType() {
            return value;
        }
    }

    private enum GcpRegion {
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

        private GcpRegion(String value, List<String> availabilityZones) {
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
