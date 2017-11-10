package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;

public class PlatformParametersTest {

    private TestPlatformParameters underTest = new TestPlatformParameters();

    @Before
    public void before() {
        underTest = new TestPlatformParameters();
    }

    @Test
    public void getRegionByNameIfValueConfigured() {
        Region defaultRegion = underTest.getDefaultRegion();
        Assert.assertEquals(Region.region("testRegion-4"), defaultRegion);
    }

    @Test
    public void getRegionByNameIfValueNotConfigured() {
        underTest.setDefaultRegionsConfigString(null);

        Region defaultRegion = underTest.getDefaultRegion();
        Assert.assertEquals(Region.region("testRegion-0"), defaultRegion);
    }

    @Test
    public void getRegionByNameIsExist() {
        Region region = underTest.getRegionByName("testRegion-0");
        Assert.assertEquals(Region.region("testRegion-0"), region);
    }

    @Test
    public void getRegionByNameIsNotExist() {
        Region region = underTest.getRegionByName("testRegion-99");
        Assert.assertEquals(null, region);
    }

    static class TestPlatformParameters implements PlatformParameters {

        private String regionDefinition = "test:testRegion-4";

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

            return new DiskTypes(Arrays.asList(diskType), diskType, stringVolumeParameterTypeMap, displayNames);
        }

        @Override
        public Regions regions() {
            List<Region> regions = new ArrayList<>();
            Region defaultRegion = null;
            for (int i = 0; i < 10; i++) {
                if (i == 0) {
                    defaultRegion = region(String.format("testRegion-%s", i));
                }
                regions.add(region(String.format("testRegion-%s", i)));
            }
            Map<Region, DisplayName> map = new HashMap<>();
            for (Region region : regions) {
                map.put(region, displayName(region.value()));
            }
            return new Regions(regions, defaultRegion, map);
        }

        @Override
        public VmTypes vmTypes(Boolean extended) {
            List<VmType> vmTypes = new ArrayList<>();
            VmType defaultVmType = null;
            for (int i = 0; i < 10; i++) {
                if (i == 0) {
                    defaultVmType = vmType(String.format("testVmType-%s", i));
                }
                vmTypes.add(vmType(String.format("testVmType-%s", i)));
            }

            return new VmTypes(vmTypes, defaultVmType);
        }

        @Override
        public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
            Map<AvailabilityZone, VmTypes> availabilityZoneVmTypesMap = new HashMap<>();
            for (AvailabilityZone availabilityZone : availabilityZones().getAllAvailabilityZone()) {
                availabilityZoneVmTypesMap.put(availabilityZone, vmTypes(false));
            }
            return availabilityZoneVmTypesMap;
        }

        @Override
        public AvailabilityZones availabilityZones() {
            Map<Region, List<AvailabilityZone>> availabiltyZones = new HashMap<>();
            for (Region region : regions().types()) {
                List<AvailabilityZone> azs = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    azs.add(new AvailabilityZone(String.format("%s-zone-%s", region.value(), i)));
                }
                availabiltyZones.put(region, azs);
            }
            return new AvailabilityZones(availabiltyZones);
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
            return new TagSpecification(1, 256, "keyValidator", 256, "valueValidator");
        }

        @Override
        public VmRecommendations recommendedVms() {
            return null;
        }

        @Override
        public String getDefaultRegionsConfigString() {
            return regionDefinition;
        }

        public void setDefaultRegionsConfigString(String regionDefinition) {
            this.regionDefinition = regionDefinition;
        }

        @Override
        public String getDefaultRegionString() {
            return "testRegion-0";
        }

        @Override
        public String platforName() {
            return "test";
        }

    }
}