package com.sequenceiq.cloudbreak.cloud.byos;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceReaderService;

@Service
public class BYOSPlatformParameters implements PlatformParameters {

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.byos.regions:}")
    private String byosRegionDefinition;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    private Map<Region, List<AvailabilityZone>> regions;

    private final Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    private Region defaultRegion;

    @PostConstruct
    public void init() {
        String zone;
        if (StringUtils.isEmpty(byosRegionDefinition)) {
            zone = resourceDefinition("zone");
        } else {
            zone = byosRegionDefinition;
        }
        regions = readRegions(zone);
        defaultRegion = getDefaultRegion();
    }

    @Override
    public Regions regions() {
        return new Regions(regions.keySet(), defaultRegion, regionDisplayNames);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public ScriptParams scriptParams() {
        return new ScriptParams("", 0);
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(Collections.emptyList(), DiskType.diskType(""), diskMappings(), new HashMap<>());
    }

    private Map<String, VolumeParameterType> diskMappings() {
        return new HashMap<>();
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        return new VmTypes(Collections.emptyList(), VmType.vmType(""));
    }

    @Override
    public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
        return Collections.emptyMap();
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("byos", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        return Collections.emptyList();
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.emptyList(), orchestrator(""));
    }

    @Override
    public PlatformImage images() {
        return new PlatformImage(new ArrayList<>(), imageRegex());
    }

    @Override
    public String imageRegex() {
        return "";
    }

    @Override
    public TagSpecification tagSpecification() {
        return null;
    }

    @Override
    public String getDefaultRegionsConfigString() {
        return defaultRegions;
    }

    @Override
    public String getDefaultRegionString() {
        return nthElement(regions.keySet(), 0).value();
    }

    @Override
    public String platforName() {
        return BYOSConstants.BYOS_PLATFORM.value();
    }
}
