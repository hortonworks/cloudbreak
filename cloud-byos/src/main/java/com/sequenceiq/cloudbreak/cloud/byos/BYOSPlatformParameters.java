package com.sequenceiq.cloudbreak.cloud.byos;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

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
public class BYOSPlatformParameters implements PlatformParameters {

    @Override
    public ScriptParams scriptParams() {
        return new ScriptParams("", 0);
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(Collections.<DiskType>emptyList(), DiskType.diskType(""));
    }

    @Override
    public Regions regions() {
        return new Regions(Collections.<Region>emptyList(), Region.region(""));
    }

    @Override
    public VmTypes vmTypes() {
        return new VmTypes(Collections.<VmType>emptyList(), VmType.vmType(""));
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(Collections.<Region, List<AvailabilityZone>>emptyMap());
    }

    @Override
    public String resourceDefinition(String resource) {
        return "";
    }
}
