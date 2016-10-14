package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class OpenStackParameters implements PlatformParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackParameters.class);

    private static final Integer START_LABEL = 97;
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("vd", START_LABEL);

    @Value("${cb.openstack.regions:}")
    private String openstackRegionDefinition;

    private Map<Region, List<AvailabilityZone>> regions = new HashMap<>();
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
        this.regions = readRegions(zone);
        this.defaultRegion = nthElement(this.regions.keySet(), 0);
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings());
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
        return new Regions(regions.keySet(), defaultRegion);
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/openstack-" + resource + ".json");
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
    public VmTypes vmTypes() {
        return new VmTypes(virtualMachines(), defaultVirtualMachine());
    }

    private Collection<VmType> virtualMachines() {
        return new ArrayList<>();
    }

    private VmType defaultVirtualMachine() {
        return vmType("");
    }
}
