package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL;
import static com.sequenceiq.cloudbreak.cloud.model.CustomImage.customImage;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.model.MachineDefinitionView;
import com.sequenceiq.cloudbreak.cloud.gcp.model.MachineDefinitionWrapper;
import com.sequenceiq.cloudbreak.cloud.gcp.model.ZoneDefinitionView;
import com.sequenceiq.cloudbreak.cloud.gcp.model.ZoneDefinitionWrapper;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.CustomImage;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionDisplayNameSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionDisplayNameSpecifications;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class GcpPlatformParameters implements PlatformParameters {

    private static final int DEFAULT_VM_TYPE_POSITION = 14;

    private static final float THOUSAND = 1000.0f;

    private static final int TEN = 10;

    private static final Integer START_LABEL = 97;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.gcp.vm.parameter.definition.path:}")
    private String gcpVmParameterDefinitionPath;

    @Value("${cb.gcp.zone.parameter.definition.path:}")
    private String gcpZoneParameterDefinitionPath;

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private Environment environment;

    @Inject
    @Qualifier("GcpTagSpecification")
    private TagSpecification tagSpecification;

    private Map<Region, List<AvailabilityZone>> regions;

    private Map<Region, DisplayName> regionDisplayNames;

    private Map<AvailabilityZone, List<VmType>> vmTypes;

    private Region defaultRegion;

    private VmType defaultVmType;

    private final Map<AvailabilityZone, VmType> defaultVmTypes = new HashMap<>();

    @PostConstruct
    public void init() {
        regions = readRegionsGcp();
        vmTypes = readVmTypes();
        regionDisplayNames = readRegionDisplayNames(resourceDefinition("zone-displaynames"));

        defaultRegion = getDefaultRegion();
        defaultVmType = nthElement(vmTypes.get(vmTypes.keySet().iterator().next()), DEFAULT_VM_TYPE_POSITION);
        initDefaultVmTypes();
    }

    private void initDefaultVmTypes() {
        for (Entry<AvailabilityZone, List<VmType>> vmType : vmTypes.entrySet()) {
            defaultVmTypes.put(vmType.getKey(), nthElement(vmType.getValue(), DEFAULT_VM_TYPE_POSITION));
        }
    }

    private Map<Region, DisplayName> readRegionDisplayNames(String displayNames) {
        Map<Region, DisplayName> regionDisplayNames = new HashMap<>();
        try {
            RegionDisplayNameSpecifications regionDisplayNameSpecifications = JsonUtil.readValue(displayNames, RegionDisplayNameSpecifications.class);
            for (RegionDisplayNameSpecification regionDisplayNameSpecification : regionDisplayNameSpecifications.getItems()) {
                regionDisplayNames.put(Region.region(regionDisplayNameSpecification.getName()),
                        displayName(regionDisplayNameSpecification.getDisplayName()));
            }
        } catch (IOException ex) {
            return regionDisplayNames;
        }
        return sortMap(regionDisplayNames);
    }

    private Map<AvailabilityZone, List<VmType>> readVmTypes() {
        Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();
        String vm = getDefinition(gcpVmParameterDefinitionPath, "vm");
        try {
            MachineDefinitionWrapper machineDefinitionWrapper = JsonUtil.readValue(vm, MachineDefinitionWrapper.class);
            for (Entry<String, Object> object : machineDefinitionWrapper.getItems().entrySet()) {
                Map value = (Map) object.getValue();
                List<Object> machineTpes = (List<Object>) value.get("machineTypes");
                for (Object machineType : machineTpes) {
                    MachineDefinitionView machineDefinitionView = new MachineDefinitionView((Map) machineType);
                    AvailabilityZone availabilityZone = new AvailabilityZone(machineDefinitionView.getZone());
                    if (!vmTypes.containsKey(availabilityZone)) {
                        List<VmType> vmTypeList = new ArrayList<>();
                        vmTypes.put(availabilityZone, vmTypeList);
                    }
                    VmTypeMeta vmTypeMeta = VmTypeMetaBuilder.builder()
                            .withCpuAndMemory(Integer.valueOf(machineDefinitionView.getGuestCpus()),
                                    Float.valueOf(machineDefinitionView.getMemoryMb()) / THOUSAND)
                            .withMagneticConfig(TEN, Integer.valueOf(machineDefinitionView.getMaximumPersistentDisksSizeGb()),
                                    1, machineDefinitionView.getMaximumNumberWithLimit())
                            .withSsdConfig(TEN, Integer.valueOf(machineDefinitionView.getMaximumPersistentDisksSizeGb()),
                                    1, machineDefinitionView.getMaximumNumberWithLimit())
                            .withMaximumPersistentDisksSizeGb(machineDefinitionView.getMaximumPersistentDisksSizeGb())
                            .create();

                    VmType vmType = VmType.vmTypeWithMeta(machineDefinitionView.getName(), vmTypeMeta, true);
                    vmTypes.get(availabilityZone).add(vmType);
                }
            }
        } catch (IOException e) {
            return vmTypes;
        }
        for (Entry<AvailabilityZone, List<VmType>> availabilityZoneListEntry : vmTypes.entrySet()) {
            availabilityZoneListEntry.getValue().sort(new StringTypesCompare());
        }
        return sortMap(vmTypes);
    }

    private Map<Region, List<AvailabilityZone>> readRegionsGcp() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        String zone = getDefinition(gcpZoneParameterDefinitionPath, "zone");
        try {
            ZoneDefinitionWrapper zoneDefinitionWrapper = JsonUtil.readValue(zone, ZoneDefinitionWrapper.class);
            for (ZoneDefinitionView object : zoneDefinitionWrapper.getItems()) {
                String region = object.getRegion();
                String avZone = object.getSelfLink();

                String[] splitRegion = region.split("/");
                String[] splitZone = avZone.split("/");

                Region regionObject = Region.region(splitRegion[splitRegion.length - 1]);
                AvailabilityZone availabilityZoneObject = AvailabilityZone.availabilityZone(splitZone[splitZone.length - 1]);
                if (!regions.keySet().contains(regionObject)) {
                    List<AvailabilityZone> availabilityZones = new ArrayList<>();
                    regions.put(regionObject, availabilityZones);
                }
                regions.get(regionObject).add(availabilityZoneObject);

            }
        } catch (IOException e) {
            return regions;
        }
        for (Entry<Region, List<AvailabilityZone>> availabilityZoneListEntry : regions.entrySet()) {
            availabilityZoneListEntry.getValue().sort(new StringTypesCompare());
        }
        return sortMap(regions);
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), diskDisplayNames());
    }

    private Map<DiskType, DisplayName> diskDisplayNames() {
        Map<DiskType, DisplayName> map = new HashMap<>();
        map.put(diskType(GcpDiskType.HDD.value()), displayName(GcpDiskType.HDD.displayName()));
        map.put(diskType(GcpDiskType.SSD.value()), displayName(GcpDiskType.SSD.displayName()));

        return map;
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(GcpDiskType.HDD.value(), VolumeParameterType.MAGNETIC);
        map.put(GcpDiskType.SSD.value(), VolumeParameterType.SSD);

        return map;
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
        return new Regions(regions.keySet(), defaultRegion, regionDisplayNames);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("gcp", resource);
    }

    private String getDefinition(String parameter, String type) {
        if (Strings.isNullOrEmpty(parameter)) {
            return resourceDefinition(type);
        } else {
            return FileReaderUtils.readFileFromClasspathQuietly(parameter);
        }
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
    public VmTypes vmTypes(Boolean extended) {
        Set<VmType> lists = new LinkedHashSet<>();
        vmTypes.values().forEach(lists::addAll);
        return new VmTypes(lists, defaultVirtualMachine());
    }

    @Override
    public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
        Map<AvailabilityZone, VmTypes> result = new HashMap<>();
        for (Entry<AvailabilityZone, List<VmType>> zoneTypes : vmTypes.entrySet()) {
            AvailabilityZone zone = zoneTypes.getKey();
            result.put(zone, new VmTypes(zoneTypes.getValue(), defaultVmTypes.get(zone)));
        }
        return result;
    }

    @Override
    public PlatformImage images() {
        List<CustomImage> customImages = new ArrayList<>();
        for (Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
            String property = environment.getProperty("gcp." + "default");
            customImages.add(customImage(regionListEntry.getKey().value(), property));
        }
        return new PlatformImage(customImages, imageRegex());
    }

    @Override
    public String imageRegex() {
        return "(.*)\\/(.*).tar.gz$";
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
        return gcpZoneParameterDefault;
    }

    @Override
    public String platforName() {
        return GcpConstants.GCP_PLATFORM.value();
    }

    private VmType defaultVirtualMachine() {
        return defaultVmType;
    }

    public enum GcpDiskType {
        SSD("pd-ssd", "Solid-state persistent disks (SSD)"),
        HDD("pd-standard", "Standard persistent disks (HDD)");

        private final String value;
        private final String displayName;

        GcpDiskType(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public static String getUrl(String projectId, AvailabilityZone zone, String volumeId) {
            return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone.value(), volumeId);
        }

        public String value() {
            return value;
        }

        public String displayName() {
            return displayName;
        }

        public String getUrl(String projectId, AvailabilityZone zone) {
            return getUrl(projectId, zone, value);
        }
    }
}
