package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL;
import static com.sequenceiq.cloudbreak.cloud.model.CustomImage.customImage;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.InstanceProfileStrategy;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.ConfigSpecification;
import com.sequenceiq.cloudbreak.cloud.model.CustomImage;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImage;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmsSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecification;
import com.sequenceiq.cloudbreak.cloud.model.ZoneVmSpecifications;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AwsPlatformParameters implements PlatformParameters {
    public static final String DEDICATED_INSTANCES = "dedicatedInstances";

    public static final String INSTANCE_PROFILE_STRATEGY = "instanceProfileStrategy";

    public static final String INSTANCE_PROFILE = "instanceProfile";

    private static final Integer START_LABEL = 97;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("xvd", START_LABEL);

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPlatformParameters.class);

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.aws.vm.parameter.definition.path:}")
    private String awsVmParameterDefinitionPath;

    @Value("${cb.aws.zone.parameter.default:eu-west-1}")
    private String awsZoneParameterDefault;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private Environment environment;

    @Inject
    @Qualifier("AwsTagSpecification")
    private TagSpecification tagSpecification;

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();

    private Map<AvailabilityZone, List<VmType>> vmTypes = new HashMap<>();

    private Map<AvailabilityZone, List<VmType>> sortListOfVmTypes = new HashMap<>();

    private Map<AvailabilityZone, VmType> defaultVmTypes = new HashMap<>();

    private Region defaultRegion;

    private VmType defaultVmType;

    @PostConstruct
    public void init() {
        this.regions = readRegions(resourceDefinition("zone"));
        readVmTypes();
        this.sortListOfVmTypes = refineList();
        this.defaultRegion = getDefaultRegion();
        this.defaultVmType = defaultVmTypes.get(regions.get(defaultRegion).get(0));
    }

    private void readVmTypes() {
        Map<String, VmType> vmTypeMap = new TreeMap<>();
        String vm = getDefinition(awsVmParameterDefinitionPath, "vm");
        String zoneVms = getDefinition(awsVmParameterDefinitionPath, "zone-vm");
        try {
            VmsSpecification oVms = JsonUtil.readValue(vm, VmsSpecification.class);
            for (VmSpecification vmSpecification : oVms.getItems()) {
                VmTypeMeta.VmTypeMetaBuilder builder = VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withCpuAndMemory(vmSpecification.getMetaSpecification().getProperties().getCpu(),
                                vmSpecification.getMetaSpecification().getProperties().getMemory())
                        .withPrice(vmSpecification.getMetaSpecification().getProperties().getPrice());
                for (ConfigSpecification configSpecification : vmSpecification.getMetaSpecification().getConfigSpecification()) {
                    addConfig(builder, configSpecification);
                }
                VmTypeMeta vmTypeMeta = builder.create();
                vmTypeMap.put(vmSpecification.getValue(), VmType.vmTypeWithMeta(vmSpecification.getValue(), vmTypeMeta, vmSpecification.getExtended()));
            }
            ZoneVmSpecifications zoneVmSpecifications = JsonUtil.readValue(zoneVms, ZoneVmSpecifications.class);
            Map<String, List<AvailabilityZone>> azmap = regions.entrySet().stream().collect(Collectors.toMap(av -> av.getKey().value(), av -> av.getValue()));
            for (ZoneVmSpecification zvs : zoneVmSpecifications.getItems()) {
                List<VmType> regionVmTypes = zvs.getVmTypes().stream().filter(vmTypeName -> vmTypeMap.containsKey(vmTypeName))
                        .map(vmTypeName -> vmTypeMap.get(vmTypeName)).collect(Collectors.toList());
                List<AvailabilityZone> azs = azmap.get(zvs.getZone());
                if (azs != null) {
                    azs.forEach(zone -> vmTypes.put(zone, regionVmTypes));
                    azs.forEach(zone -> defaultVmTypes.put(zone, vmTypeMap.get(zvs.getDefaultVmType())));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Cannot initialize platform parameters for aws", e);
        }
    }

    private Map<AvailabilityZone, List<VmType>> refineList() {
        Map<AvailabilityZone, List<VmType>> resultMap = new HashMap<>();
        for (Map.Entry<AvailabilityZone, List<VmType>> availabilityZoneListEntry : this.vmTypes.entrySet()) {
            List<VmType> tmpList = new ArrayList<>();
            for (VmType vmType : availabilityZoneListEntry.getValue()) {
                if (!vmType.getExtended()) {
                    tmpList.add(vmType);
                }
            }
            resultMap.put(availabilityZoneListEntry.getKey(), tmpList);
        }
        return sortMap(resultMap);
    }

    private void addConfig(VmTypeMeta.VmTypeMetaBuilder builder, ConfigSpecification configSpecification) {
        if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.AUTO_ATTACHED.name())) {
            builder.withAutoAttachedConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.EPHEMERAL.name())) {
            builder.withEphemeralConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.MAGNETIC.name())) {
            builder.withMagneticConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.SSD.name())) {
            builder.withSsdConfig(volumeParameterConfig(configSpecification));
        } else if (configSpecification.getVolumeParameterType().equals(VolumeParameterType.ST1.name())) {
            builder.withSt1Config(volumeParameterConfig(configSpecification));
        }
    }

    private VolumeParameterConfig volumeParameterConfig(ConfigSpecification configSpecification) {
        return new VolumeParameterConfig(
                VolumeParameterType.valueOf(configSpecification.getVolumeParameterType()),
                Integer.valueOf(configSpecification.getMinimumSize()),
                Integer.valueOf(configSpecification.getMaximumSize()),
                Integer.valueOf(configSpecification.getMinimumNumber()),
                configSpecification.getMaximumNumberWithLimit());
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
        map.put(AwsDiskType.St1.value, VolumeParameterType.ST1);
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
        return cloudbreakResourceReaderService.resourceDefinition("aws", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(TTL, false, String.class, Optional.absent()));
        additionalStackParameterValidations.add(new StackParamValidation(DEDICATED_INSTANCES, false, Boolean.class, Optional.absent()));
        additionalStackParameterValidations.add(new StackParamValidation(INSTANCE_PROFILE_STRATEGY, false, InstanceProfileStrategy.class,
                Optional.absent()));
        additionalStackParameterValidations.add(new StackParamValidation(INSTANCE_PROFILE, false, String.class, Optional.absent()));
        return additionalStackParameterValidations;
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        Set<VmType> lists = new TreeSet<>(new Comparator<VmType>() {
            @Override
            public int compare(VmType o1, VmType o2) {
                return o1.value().compareTo(o2.value());
            }
        });
        if (extended) {
            for (List<VmType> vmTypeList : vmTypes.values()) {
                lists.addAll(vmTypeList);
            }
        } else {
            for (List<VmType> vmTypeList : sortListOfVmTypes.values()) {
                lists.addAll(vmTypeList);
            }
        }
        return new VmTypes(lists, defaultVmType);
    }

    @Override
    public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
        Map<AvailabilityZone, List<VmType>> map = extended ? vmTypes : sortListOfVmTypes;
        return map.entrySet().stream().collect(
                Collectors.toMap(vmt -> vmt.getKey(), vmt -> new VmTypes(vmt.getValue(), defaultVmTypes.get(vmt.getKey()))));
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public PlatformImage images() {
        List<CustomImage> customImages = new ArrayList<>();
        for (Map.Entry<Region, List<AvailabilityZone>> regionListEntry : regions.entrySet()) {
            String property = environment.getProperty("aws." + regionListEntry.getKey().value());
            customImages.add(customImage(regionListEntry.getKey().value(), property));
        }
        return new PlatformImage(customImages, imageRegex());
    }

    @Override
    public String imageRegex() {
        return "^ami-[a-zA-Z0-9]{8}$";
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
        return awsZoneParameterDefault;
    }

    @Override
    public String platforName() {
        return AwsConstants.AWS_PLATFORM.value();
    }

    public enum AwsDiskType {
        Standard("standard"),
        Ephemeral("ephemeral"),
        Gp2("gp2"),
        St1("st1");

        private final String value;

        AwsDiskType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
