package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
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
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class GcpPlatformParameters implements PlatformParameters {
    private static final int DEFAULT_REGION_TYPE_POSITION = 2;
    private static final int DEFAULT_VM_TYPE_POSITION = 14;
    private static final float THOUSAND = 1000.0f;
    private static final int TEN = 10;
    private static final Integer START_LABEL = 97;
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    @Value("${cb.gcp.vm.parameter.definition.path:}")
    private String gcpVmParameterDefinitionPath;

    @Value("${cb.gcp.zone.parameter.definition.path:}")
    private String gcpZoneParameterDefinitionPath;

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
    private Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();
    private Region defaultRegion;
    private VmType defaultVmType;

    @PostConstruct
    public void init() {
        this.regions = readRegions();
        this.vmTypes = readVmTypes();

        this.defaultRegion = nthElement(this.regions.keySet(), DEFAULT_REGION_TYPE_POSITION);
        this.defaultVmType = nthElement(this.vmTypes.get(this.vmTypes.keySet().iterator().next()), DEFAULT_VM_TYPE_POSITION);
    }

    private Map<AvailabilityZone, List<VmType>> readVmTypes() {
        Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();
        String vm = getDefinition(gcpVmParameterDefinitionPath, "vm");
        try {
            MachineDefinitionWrapper machineDefinitionWrapper = JsonUtil.readValue(vm, MachineDefinitionWrapper.class);
            for (Map.Entry<String, Object> object : machineDefinitionWrapper.getItems().entrySet()) {
                Map value = (Map) object.getValue();
                List<Object> machineTpes = (List<Object>) value.get("machineTypes");
                for (Object machineType : machineTpes) {
                    MachineDefinitionView machineDefinitionView = new MachineDefinitionView((Map) machineType);
                    AvailabilityZone availabilityZone = new AvailabilityZone(machineDefinitionView.getZone());
                    if (!vmTypes.containsKey(availabilityZone)) {
                        List<VmType> vmTypeList = new ArrayList<>();
                        vmTypes.put(availabilityZone, vmTypeList);
                    }
                    VmTypeMeta vmTypeMeta = VmTypeMeta.VmTypeMetaBuilder.builder()
                            .withCpuAndMemory(Integer.valueOf(machineDefinitionView.getGuestCpus()),
                                    Float.valueOf(machineDefinitionView.getMemoryMb()) / THOUSAND)
                            .withMagneticConfig(TEN, Integer.valueOf(machineDefinitionView.getMaximumPersistentDisksSizeGb()),
                                    1, machineDefinitionView.getMaximumNumberWithLimit())
                            .withSsdConfig(TEN, Integer.valueOf(machineDefinitionView.getMaximumPersistentDisksSizeGb()),
                                    1, machineDefinitionView.getMaximumNumberWithLimit())
                            .withMaximumPersistentDisksSizeGb(machineDefinitionView.getMaximumPersistentDisksSizeGb())
                            .create();

                    VmType vmType  = VmType.vmTypeWithMeta(machineDefinitionView.getName(), vmTypeMeta, true);
                    vmTypes.get(availabilityZone).add(vmType);
                }
            }
        } catch (IOException e) {
            return vmTypes;
        }
        for (Map.Entry<AvailabilityZone, List<VmType>> availabilityZoneListEntry : vmTypes.entrySet()) {
            Collections.sort(availabilityZoneListEntry.getValue(), new StringTypesCompare());
        }
        return sortMap(vmTypes);
    }

    private Map<Region, List<AvailabilityZone>> readRegions() {
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
        for (Map.Entry<Region, List<AvailabilityZone>> availabilityZoneListEntry : regions.entrySet()) {
            Collections.sort(availabilityZoneListEntry.getValue(), new StringTypesCompare());
        }
        return sortMap(regions);
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings());
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
        return new Regions(regions.keySet(), defaultRegion);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/gcp-" + resource + ".json");
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
        return Collections.emptyList();
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

    private VmType defaultVirtualMachine() {
        return defaultVmType;
    }

    public enum GcpDiskType {
        SSD("pd-ssd"), HDD("pd-standard");

        private final String value;

        GcpDiskType(String value) {
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
}
