package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;

@Service
public class MockPlatformParameters implements PlatformParameters {

    private enum MockedVmTypes {

        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        private final String value;

        MockedVmTypes(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public VmTypeMeta getVmTypeMeta() {
            VmTypeMeta vmTypeMeta = new VmTypeMeta();
            vmTypeMeta.setSsdConfig(getVolumeConfig(VolumeParameterType.SSD));
            vmTypeMeta.setEphemeralConfig(getVolumeConfig(VolumeParameterType.EPHEMERAL));
            vmTypeMeta.setMagneticConfig(getVolumeConfig(VolumeParameterType.MAGNETIC));
            vmTypeMeta.setAutoAttachedConfig(getVolumeConfig(VolumeParameterType.AUTO_ATTACHED));
            return vmTypeMeta;
        }

        private VolumeParameterConfig getVolumeConfig(VolumeParameterType ssd) {
            return new VolumeParameterConfig(ssd, 1, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        }
    }

    private static final Integer START_LABEL = 1;
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("mockdisk", START_LABEL);
    private static final String MOCK_RESOURCE_DEFINITION = "{}";
    private static final String[] EUROPE_AVAILABILITY_ZONES = {"europe-a", "europe-b"};
    private static final String[] USA_AVAILABILITY_ZONES = {"usa-a", "usa-b", "usa-c"};

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
    private Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();
    private Region defaultRegion;
    private VmType defaultVmType;

    @PostConstruct
    public void init() {
        this.regions = readRegions();
        this.vmTypes = readVmTypes();
        this.defaultRegion = this.regions.keySet().iterator().next();
        this.defaultVmType = this.vmTypes.get(this.vmTypes.keySet().iterator().next()).get(0);
    }

    private Map<Region, List<AvailabilityZone>> readRegions() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        regions.put(Region.region("USA"), getAvailabilityZones(USA_AVAILABILITY_ZONES));
        regions.put(Region.region("Europe"), getAvailabilityZones(EUROPE_AVAILABILITY_ZONES));
        return regions;
    }

    private List<AvailabilityZone> getAvailabilityZones(String[] availabilityZones) {
        List<AvailabilityZone> availabilityZoneList = new ArrayList<>();
        for (String availabilityZone : availabilityZones) {
            availabilityZoneList.add(new AvailabilityZone(availabilityZone));
        }
        return availabilityZoneList;
    }

    private Map<AvailabilityZone, List<VmType>> readVmTypes() {
        Map<AvailabilityZone, List<VmType>> availabilityZoneListHashMap = new HashMap<>();
        List<AvailabilityZone> availabilityZoneList = new ArrayList<>();
        availabilityZoneList.addAll(getAvailabilityZones(USA_AVAILABILITY_ZONES));
        availabilityZoneList.addAll(getAvailabilityZones(EUROPE_AVAILABILITY_ZONES));

        List<VmType> vmTypeList = new ArrayList<>();
        for (MockedVmTypes vmType : MockedVmTypes.values()) {
            vmTypeList.add(VmType.vmTypeWithMeta(vmType.value, vmType.getVmTypeMeta(), true));
        }

        for (AvailabilityZone availabilityZone : availabilityZoneList) {
            availabilityZoneListHashMap.put(availabilityZone, vmTypeList);
        }
        return availabilityZoneListHashMap;
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        Map<String, VolumeParameterType> diskMappings = new HashMap<>();
        diskMappings.put(MockDiskType.MAGNETIC_DISK.value(), VolumeParameterType.MAGNETIC);
        diskMappings.put(MockDiskType.SSD.value(), VolumeParameterType.SSD);
        diskMappings.put(MockDiskType.EPHEMERAL.value(), VolumeParameterType.EPHEMERAL);
        return new DiskTypes(getDiskTypes(), getDefaultDiskType(), diskMappings);
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (MockDiskType diskType : MockDiskType.values()) {
            disks.add(diskType(diskType.value));
        }
        return disks;
    }

    private DiskType getDefaultDiskType() {
        return diskType(MockDiskType.MAGNETIC_DISK.value());
    }

    @Override
    public Regions regions() {
        return new Regions(regions.keySet(), defaultRegion);
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        Set<VmType> lists = new LinkedHashSet<>();
        for (List<VmType> vmTypeList : vmTypes.values()) {
            lists.addAll(vmTypeList);
        }
        return new VmTypes(lists, defaultVmType);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return MOCK_RESOURCE_DEFINITION;
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        return new ArrayList<>();
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Arrays.asList(orchestrator(OrchestratorConstants.SALT), orchestrator(OrchestratorConstants.SWARM)),
                orchestrator(OrchestratorConstants.SWARM));
    }

    private enum MockDiskType {
        MAGNETIC_DISK("magnetic"),
        SSD("ssd"),
        EPHEMERAL("ephemeral");

        private final String value;

        MockDiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
