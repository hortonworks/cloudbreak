package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class MockPlatformParameters implements PlatformParameters {

    private static final Integer START_LABEL = 1;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("mockdisk", START_LABEL);

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

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
        return new DiskTypes(getDiskTypes(), getDefaultDiskType(), diskMappings, new HashMap<>());
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
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("mock", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        return new ArrayList<>();
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singleton(orchestrator(OrchestratorConstants.SALT)),
                orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public TagSpecification tagSpecification() {
        return null;
    }

    @Override
    public String platforName() {
        return MockConstants.MOCK;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return null;
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
