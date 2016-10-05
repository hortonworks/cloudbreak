package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.ConfigSpecification;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionSpecification;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.RegionsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.StringTypesCompare;
import com.sequenceiq.cloudbreak.cloud.model.VmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class ArmPlatformParameters implements PlatformParameters {

    private static final int START_LABEL = 98;
    private static final int DEFAULT_REGION_TYPE_POSITION = 4;
    private static final int DEFAULT_VM_TYPE_POSITION = 1;
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    @Value("${cb.arm.vm.parameter.definition.path:}")
    private String armVmParameterDefinitionPath;

    @Value("${cb.arm.zone.parameter.definition.path:}")
    private String armZoneParameterDefinitionPath;

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
    private List<VmType> vmTypes = new ArrayList<>();
    private Region defaultRegion;
    private VmType defaultVmType;

    @PostConstruct
    public void init() {
        this.regions = readRegions();
        this.vmTypes = readVmTypes();
        this.defaultRegion = nthElement(this.regions.keySet(), DEFAULT_REGION_TYPE_POSITION);
        this.defaultVmType = nthElement(this.vmTypes, DEFAULT_VM_TYPE_POSITION);
    }

    private List<VmType> readVmTypes() {
        List<VmType> vmTypes = new ArrayList<>();
        String vm = getDefinition(armVmParameterDefinitionPath, "vm");
        try {
            VmsSpecification oVms = JsonUtil.readValue(vm, VmsSpecification.class);
            for (VmSpecification vmSpecification : oVms.getItems()) {

                VmTypeMeta.VmTypeMetaBuilder builder = VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(vmSpecification.getMetaSpecification().getProperties().getCpu(),
                                vmSpecification.getMetaSpecification().getProperties().getMemory());

                for (ConfigSpecification configSpecification : vmSpecification.getMetaSpecification().getConfigSpecification()) {
                    if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.AUTO_ATTACHED.name())) {
                        builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification));
                    } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.EPHEMERAL.name())) {
                        builder.withEphemeralConfig(volumeParameterConfig(configSpecification));
                    } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.MAGNETIC.name())) {
                        builder.withMagneticConfig(volumeParameterConfig(configSpecification));
                    } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.SSD.name())) {
                        builder.withSsdConfig(volumeParameterConfig(configSpecification));
                    }
                }
                VmTypeMeta vmTypeMeta = builder.create();
                vmTypes.add(VmType.vmTypeWithMeta(vmSpecification.getValue(), vmTypeMeta, true));
            }
        } catch (IOException e) {
            return vmTypes;
        }
        Collections.sort(vmTypes, new StringTypesCompare());
        return vmTypes;
    }

    private VolumeParameterConfig volumeParameterConfig(ConfigSpecification configSpecification) {
        return new VolumeParameterConfig(
                VolumeParameterType.valueOf(configSpecification.getVolumeParameterType()),
                Integer.valueOf(configSpecification.getMinimumSize()),
                Integer.valueOf(configSpecification.getMaximumSize()),
                Integer.valueOf(configSpecification.getMinimumNumber()),
                configSpecification.getMaximumNumberWithLimit());
    }

    private Map<Region, List<AvailabilityZone>> readRegions() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        String zone = getDefinition(armZoneParameterDefinitionPath, "zone");
        try {
            RegionsSpecification oRegions = JsonUtil.readValue(zone, RegionsSpecification.class);
            for (RegionSpecification regionSpecification : oRegions.getItems()) {
                List<AvailabilityZone> av = new ArrayList<>();
                for (String s : regionSpecification.getZones()) {
                    av.add(AvailabilityZone.availabilityZone(s));
                }
                Collections.sort(av, new StringTypesCompare());
                regions.put(Region.region(regionSpecification.getName()), av);
            }
        } catch (IOException e) {
            return regions;
        }
        return sortMap(regions);
    }

    private String getDefinition(String parameter, String type) {
        if (Strings.isNullOrEmpty(parameter)) {
            return resourceDefinition(type);
        } else {
            return FileReaderUtils.readFileFromClasspathQuietly(parameter);
        }
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings());
    }

    @Override
    public Regions regions() {
        return new Regions(regions.keySet(), defaultRegion);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (ArmDiskType diskType : ArmDiskType.values()) {
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(ArmDiskType.GEO_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        map.put(ArmDiskType.LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        map.put(ArmDiskType.PREMIUM_LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        return map;
    }

    private DiskType defaultDiskType() {
        return diskType(ArmDiskType.LOCALLY_REDUNDANT.value());
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/arm-" + resource + ".json");
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation("diskPerStorage", false, String.class, Optional.absent()));
        additionalStackParameterValidations.add(new StackParamValidation("persistentStorage", false, String.class, Optional.of("^[a-z0-9]{0,24}$")));
        additionalStackParameterValidations.add(new StackParamValidation("attachedStorageOption", false, ArmAttachedStorageOption.class,
                Optional.absent()));
        return additionalStackParameterValidations;
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        return new VmTypes(vmTypes, defaultVmType);
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
    }
}
