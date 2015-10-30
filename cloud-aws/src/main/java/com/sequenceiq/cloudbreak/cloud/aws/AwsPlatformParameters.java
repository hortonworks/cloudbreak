package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;

@Service
public class AwsPlatformParameters implements PlatformParameters {

    private static final Integer START_LABEL = Integer.valueOf(97);

    @Override
    public String diskPrefix() {
        return "xvd";
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
        return diskTypes().get(DiskType.Standard);
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
        return Region.US_WEST_1.name();
    }

    @Override
    public Map<String, List<String>> availabiltyZones() {
        Map<String, List<String>> regions = new HashMap<>();
        for (Region region : Region.values()) {
            regions.put(region.name(), region.availabilityZones());
        }
        return regions;
    }

    @Override
    public Map<String, String> virtualMachines() {
        Map<String, String> vms = new HashMap<>();
        for (InstanceType instanceType : InstanceType.values()) {
            vms.put(instanceType.name(), instanceType.toString());
        }
        return vms;
    }

    @Override
    public String defaultVirtualMachine() {
        return InstanceType.M1Large.name();
    }

    private enum DiskType {
        Standard("standard"),
        Ephemeral("ephemeral"),
        Gp2("gp2");

        private final String value;

        private DiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private enum Region {
        GovCloud("us-gov-west-1", new ArrayList<String>()),
        US_EAST_1("us-east-1", Arrays.asList("us-east-1a", "us-east-1b", "us-east-1d", "us-east-1e")),
        US_WEST_1("us-west-1", Arrays.asList("us-west-1a", "us-west-1b")),
        US_WEST_2("us-west-2", Arrays.asList("us-west-2a", "us-west-2b", "us-west-2c")),
        EU_WEST_1("eu-west-1", Arrays.asList("eu-west-1a", "eu-west-1b", "eu-west-1c")),
        EU_CENTRAL_1("eu-central-1", Arrays.asList("eu-central-1a", "eu-central-1b")),
        AP_SOUTHEAST_1("ap-southeast-1", Arrays.asList("ap-southeast-1a", "ap-southeast-1b")),
        AP_SOUTHEAST_2("ap-southeast-2", Arrays.asList("ap-southeast-2a", "ap-southeast-2b")),
        AP_NORTHEAST_1("ap-northeast-1", Arrays.asList("ap-northeast-1a", "ap-northeast-1c")),
        SA_EAST_1("sa-east-1", Arrays.asList("sa-east-1a", "sa-east-1b", "sa-east-1c")),
        CN_NORTH_1("cn-north-1", new ArrayList<String>());

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
