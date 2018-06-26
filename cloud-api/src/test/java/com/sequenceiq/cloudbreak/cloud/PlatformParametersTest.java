package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;

public class PlatformParametersTest {

    private TestPlatformParameters underTest = new TestPlatformParameters();

    @Before
    public void before() {
        underTest = new TestPlatformParameters();
    }

    @Test
    public void getRegionByNameIfValueNotConfigured() {
        DiskTypes diskTypes = underTest.diskTypes();
        Assert.assertEquals(1L, diskTypes.displayNames().entrySet().size());
        Assert.assertEquals(1L, diskTypes.diskMapping().entrySet().size());
        Assert.assertEquals("testDiskType", diskTypes.defaultType().value());
    }

    static class TestPlatformParameters implements PlatformParameters {

        @Override
        public ScriptParams scriptParams() {
            return new ScriptParams("testDiskPrefix", 1);
        }

        @Override
        public DiskTypes diskTypes() {
            DiskType diskType = diskType("testDiskType");

            Map<String, VolumeParameterType> stringVolumeParameterTypeMap = new HashMap<>();
            stringVolumeParameterTypeMap.put("testparameter", VolumeParameterType.EPHEMERAL);

            Map<DiskType, DisplayName> displayNames = new HashMap<>();
            displayNames.put(diskType, displayName("diskType"));

            return new DiskTypes(Collections.singletonList(diskType), diskType, stringVolumeParameterTypeMap, displayNames);
        }

        @Override
        public String resourceDefinition(String resource) {
            return "testResourceDefinition";
        }

        @Override
        public List<StackParamValidation> additionalStackParameters() {
            return Lists.newArrayList();
        }

        @Override
        public PlatformOrchestrator orchestratorParams() {
            return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
        }

        @Override
        public TagSpecification tagSpecification() {
            return new TagSpecification(1, 5, 256, "keyValidator", 5, 256, "valueValidator");
        }

        @Override
        public VmRecommendations recommendedVms() {
            return null;
        }

        @Override
        public String platforName() {
            return "test";
        }
    }
}
