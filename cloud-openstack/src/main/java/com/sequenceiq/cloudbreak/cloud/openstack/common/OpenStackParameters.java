package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL;
import static com.sequenceiq.cloudbreak.cloud.model.CustomImage.customImage;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.CustomImage;
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
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;

@Service
public class OpenStackParameters implements PlatformParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackParameters.class);

    private static final Integer START_LABEL = 97;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("vd", START_LABEL);

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.openstack.regions:}")
    private String openstackRegionDefinition;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private Environment environment;

    @Inject
    @Qualifier("OpenStackTagSpecification")
    private TagSpecification tagSpecification;

    private Map<Region, List<AvailabilityZone>> regions;

    private final Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    private Region defaultRegion;

    @PostConstruct
    public void init() {
        String zone;
        if (StringUtils.isEmpty(openstackRegionDefinition)) {
            zone = resourceDefinition("zone");
        } else {
            zone = openstackRegionDefinition;
        }
        LOGGER.info("Zone definition for OpenStack: {}", zone);
        regions = readRegions(zone);
        defaultRegion = getDefaultRegion();
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), new HashMap<>());
    }

    private Collection<DiskType> getDiskTypes() {
        return new ArrayList<>();
    }

    private DiskType defaultDiskType() {
        return diskType("HDD");
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put("HDD", VolumeParameterType.MAGNETIC);
        return map;
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
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("openstack", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(TTL, false, String.class, Optional.empty()));
        return additionalStackParameterValidations;
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public PlatformImage images() {
        List<CustomImage> customImages = new ArrayList<>();
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
            String property = environment.getProperty("openstack." + "default");
            customImages.add(customImage(regionListEntry.getKey().value(), property));
        }
        return new PlatformImage(customImages, imageRegex());
    }

    @Override
    public String imageRegex() {
        return "";
    }

    @Override
    public TagSpecification tagSpecification() {
        return tagSpecification;
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
        return OpenStackConstants.OPENSTACK_PLATFORM.value();
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        return new VmTypes(virtualMachines(extended), defaultVirtualMachine());
    }

    @Override
    public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
        Map<AvailabilityZone, VmTypes> result = new HashMap<>();
        for (Entry<Region, List<AvailabilityZone>> zones : regions.entrySet()) {
            for (AvailabilityZone zone : zones.getValue()) {
                result.put(zone, new VmTypes(virtualMachines(extended), defaultVirtualMachine()));
            }
        }
        return result;
    }

    private Collection<VmType> virtualMachines(Boolean extended) {
        return new ArrayList<>();
    }

    private VmType defaultVirtualMachine() {
        return vmType("");
    }
}
