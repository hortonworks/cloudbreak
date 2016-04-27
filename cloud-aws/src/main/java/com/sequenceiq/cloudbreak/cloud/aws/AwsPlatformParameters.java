package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
public class AwsPlatformParameters implements PlatformParameters {
    public static final VolumeParameterConfig EBS_MAGNETIC_CONFIG = new VolumeParameterConfig(VolumeParameterType.MAGNETIC, 1, 1024, 1, 24);
    public static final VolumeParameterConfig EBS_SSD_CONFIG = new VolumeParameterConfig(VolumeParameterType.SSD, 1, 17592, 1, 24);

    private static final Integer START_LABEL = Integer.valueOf(97);
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("xvd", START_LABEL);

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    @Value("${cb.aws.zone.parameter.definition.path:}")
    private String awsZoneParameterDefinitionPath;

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

    private Map<AvailabilityZone, List<VmType>> readVmTypes() {
        Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();
        List<VmType> tmpVmTypes = new ArrayList<>();
        String vm = getDefinition(awsVmParameterDefinitionPath, "vm");
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
                tmpVmTypes.add(VmType.vmTypeWithMeta(vmSpecification.getValue(), vmTypeMeta));
            }
            for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
                for (AvailabilityZone availabilityZone : regionListEntry.getValue()) {
                    vmTypes.put(availabilityZone, tmpVmTypes);
                }
            }
        } catch (IOException e) {
            return vmTypes;
        }
        return vmTypes;
    }

    private VolumeParameterConfig volumeParameterConfig(ConfigSpecification configSpecification) {
        return new VolumeParameterConfig(
                VolumeParameterType.valueOf(configSpecification.getVolumeParameterType()),
                Integer.valueOf(configSpecification.getMinimumSize()),
                Integer.valueOf(configSpecification.getMaximumSize()),
                Integer.valueOf(configSpecification.getMinimumNumber()),
                Integer.valueOf(configSpecification.getMaximumNumberWithLimit()));
    }

    private Map<Region, List<AvailabilityZone>> readRegions() {
        Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
        String zone = getDefinition(awsZoneParameterDefinitionPath, "zone");
        try {
            RegionsSpecification oRegions = JsonUtil.readValue(zone, RegionsSpecification.class);
            for (RegionSpecification regionSpecification : oRegions.getItems()) {
                List<AvailabilityZone> av = new ArrayList<>();
                for (String s : regionSpecification.getZones()) {
                    av.add(AvailabilityZone.availabilityZone(s));
                }
                regions.put(Region.region(regionSpecification.getName()), av);
            }
        } catch (IOException e) {
            return regions;
        }
        return regions;
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

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(AwsDiskType.Standard.value, VolumeParameterType.MAGNETIC);
        map.put(AwsDiskType.Gp2.value, VolumeParameterType.SSD);
        map.put(AwsDiskType.Ephemeral.value, VolumeParameterType.EPHEMERAL);
        return map;
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
        return new Regions(regions.keySet(), defaultRegion);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/aws-" + resource + ".json");
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation("dedicatedInstances", false, Boolean.class, Optional.<String>absent()));
        return additionalStackParameterValidations;
    }

    @Override
    public VmTypes vmTypes() {
        Set<VmType> lists = new LinkedHashSet<>();
        for (List<VmType> vmTypeList : vmTypes.values()) {
            lists.addAll(vmTypeList);
        }
        return new VmTypes(lists, defaultVmType);
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Arrays.asList(orchestrator(OrchestratorConstants.ON_HOST), orchestrator(OrchestratorConstants.SWARM)),
                orchestrator(OrchestratorConstants.ON_HOST));
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

}
