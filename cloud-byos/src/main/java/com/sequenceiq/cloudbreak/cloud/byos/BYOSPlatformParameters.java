package com.sequenceiq.cloudbreak.cloud.byos;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;

@Service
public class BYOSPlatformParameters implements PlatformParameters {

    @Override
    public ScriptParams scriptParams() {
        return new ScriptParams("", 0);
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(Collections.emptyList(), DiskType.diskType(""), diskMappings());
    }

    private Map<String, VolumeParameterType> diskMappings() {
        return new HashMap<>();
    }

    @Override
    public Regions regions() {
        return new Regions(Collections.emptyList(), Region.region(""));
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        return new VmTypes(Collections.emptyList(), VmType.vmType(""));
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(Collections.emptyMap());
    }

    @Override
    public String resourceDefinition(String resource) {
        return "";
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        return Collections.emptyList();
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.emptyList(), orchestrator(""));
    }
}
