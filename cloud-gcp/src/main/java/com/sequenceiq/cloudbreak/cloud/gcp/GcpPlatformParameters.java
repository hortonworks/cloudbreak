package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

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
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    private DiskType defaultDiskType() {
        return diskType(GcpDiskType.HDD.value());
    }

    @Override
    public Regions regions() {
        return new Regions(getRegions(), defaultRegion());
    }

    private Collection<Region> getRegions() {
        Collection<Region> regions = Lists.newArrayList();
        for (GcpRegion region : GcpRegion.values()) {
            regions.add(region.region());
        }
        return regions;
    }

    private Region defaultRegion() {
        return GcpRegion.US_CENTRAL.region();
    }

    @Override
    public AvailabilityZones availabilityZones() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        for (GcpRegion region : GcpRegion.values()) {
            regions.put(region.region(), region.availabilityZones());
        }
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/gcp-" + resource + ".json");
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        return Collections.emptyList();
    }

    @Override
    public VmTypes vmTypes() {
        return new VmTypes(virtualMachines(), defaultVirtualMachine());
    }

    private Collection<VmType> virtualMachines() {
        Collection<VmType> vmTypes = Lists.newArrayList();
        for (GcpVmType vmType : GcpVmType.values()) {
            vmTypes.add(vmType(vmType.value));
        }
        return vmTypes;
    }

    private VmType defaultVirtualMachine() {
        return vmType(GcpVmType.N1_STANDARD_2.value);
    }

    public enum GcpDiskType {
        SSD("pd-ssd"), HDD("pd-standard");

        private final String value;

        private GcpDiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public String getUrl(String projectId, AvailabilityZone zone) {
            return getUrl(projectId, zone, value);
        }

        public static String getUrl(String projectId, AvailabilityZone zone, String volumeId) {
            return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), volumeId);
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
        N1_HIGHCPU_16("n1-highcpu-16");

        private final String value;

        private GcpVmType(String value) {
            this.value = value;
        }
    }

    private enum GcpRegion {
        US_CENTRAL(Region.region("us-central1"), Arrays.asList(availabilityZone("us-central1-a"),
                availabilityZone("us-central1-b"),
                availabilityZone("us-central1-c"),
                availabilityZone("us-central1-f"))),
        US_EAST(Region.region("us-east1"), Arrays.asList(availabilityZone("us-east1-b"),
                availabilityZone("us-east1-c"),
                availabilityZone("us-east1-d"))),
        EUROPE_WEST(Region.region("europe-west1"), Arrays.asList(availabilityZone("europe-west1-b"),
                availabilityZone("europe-west1-c"),
                availabilityZone("europe-west1-d"))),
        ASIA_EAST1(Region.region("asia-east1"), Arrays.asList(availabilityZone("asia-east1-a"),
                availabilityZone("asia-east1-b"),
                availabilityZone("asia-east1-c")));

        private final Region region;
        private final List<AvailabilityZone> availabilityZones;

        private GcpRegion(Region region, List<AvailabilityZone> availabilityZones) {
            this.region = region;
            this.availabilityZones = availabilityZones;
        }

        public Region region() {
            return this.region;
        }

        public List<AvailabilityZone> availabilityZones() {
            return this.availabilityZones;
        }

    }
}
