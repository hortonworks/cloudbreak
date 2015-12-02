package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class AwsPlatformParameters implements PlatformParameters {
    private static final Integer START_LABEL = Integer.valueOf(97);
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("xvd", START_LABEL);

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
        for (AwsDiskType diskType : AwsDiskType.values()) {
            disks.add(diskType(diskType.value));
        }
        return disks;
    }

    private DiskType defaultDiskType() {
        return diskType(AwsDiskType.Standard.value());
    }

    @Override
    public Regions regions() {
        return new Regions(getRegions(), defaultRegion());
    }

    private Collection<Region> getRegions() {
        Collection<Region> regions = Lists.newArrayList();
        for (AwsRegion region : AwsRegion.values()) {
            regions.add(region(region.value()));
        }
        return regions;
    }

    private Region defaultRegion() {
        return region(AwsRegion.US_WEST_1.value());
    }

    @Override
    public AvailabilityZones availabilityZones() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        for (AwsRegion region : AwsRegion.values()) {
            regions.put(region(region.value), region.availabilityZones());
        }
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/aws-" + resource + ".json");
    }

    @Override
    public VmTypes vmTypes() {
        return new VmTypes(virtualMachines(), defaultVirtualMachine());
    }

    private Collection<VmType> virtualMachines() {
        Collection<VmType> vms = Lists.newArrayList();
        for (AwsInstanceType instanceType : AwsInstanceType.values()) {
            vms.add(VmType.vmTypeWithMeta(instanceType.toString(), instanceType.getMeta()));
        }
        return vms;
    }

    private VmType defaultVirtualMachine() {
        return VmType.vmType(AwsInstanceType.M3Large.toString());
    }

    private enum AwsDiskType {
        Standard("standard"),
        Ephemeral("ephemeral"),
        Gp2("gp2");

        private final String value;

        private AwsDiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private enum AwsRegion {
        GovCloud("us-gov-west-1", new ArrayList<AvailabilityZone>()),
        US_EAST_1("us-east-1", Arrays.asList(availabilityZone("us-east-1a"), availabilityZone("us-east-1b"),
                availabilityZone("us-east-1d"), availabilityZone("us-east-1e"))),
        US_WEST_1("us-west-1", Arrays.asList(availabilityZone("us-west-1a"), availabilityZone("us-west-1b"))),
        US_WEST_2("us-west-2", Arrays.asList(availabilityZone("us-west-2a"), availabilityZone("us-west-2b"), availabilityZone("us-west-2c"))),
        EU_WEST_1("eu-west-1", Arrays.asList(availabilityZone("eu-west-1a"), availabilityZone("eu-west-1b"), availabilityZone("eu-west-1c"))),
        EU_CENTRAL_1("eu-central-1", Arrays.asList(availabilityZone("eu-central-1a"), availabilityZone("eu-central-1b"))),
        AP_SOUTHEAST_1("ap-southeast-1", Arrays.asList(availabilityZone("ap-southeast-1a"), availabilityZone("ap-southeast-1b"))),
        AP_SOUTHEAST_2("ap-southeast-2", Arrays.asList(availabilityZone("ap-southeast-2a"), availabilityZone("ap-southeast-2b"))),
        AP_NORTHEAST_1("ap-northeast-1", Arrays.asList(availabilityZone("ap-northeast-1a"), availabilityZone("ap-northeast-1c"))),
        SA_EAST_1("sa-east-1", Arrays.asList(availabilityZone("sa-east-1a"), availabilityZone("sa-east-1b"), availabilityZone("sa-east-1c"))),
        CN_NORTH_1("cn-north-1", new ArrayList<AvailabilityZone>());

        private final String value;
        private final List<AvailabilityZone> availabilityZones;

        private AwsRegion(String value, List<AvailabilityZone> availabilityZones) {
            this.value = value;
            this.availabilityZones = availabilityZones;
        }

        public String value() {
            return this.value;
        }

        public List<AvailabilityZone> availabilityZones() {
            return this.availabilityZones;
        }
    }

}
