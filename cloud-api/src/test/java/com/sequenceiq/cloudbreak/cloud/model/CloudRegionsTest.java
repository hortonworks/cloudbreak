package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CloudRegionsTest {

    private static final Region EU_CENTRAL_1 = region("eu-central-1");

    private static final Region US_EAST_1 = region("us-east-1");

    private static final List<String> X86_INSTANCE_TYPE = List.of("m5.2xlarge");

    private static final List<String> ARM_INSTANCE_TYPE = List.of("m6g.2xlarge");

    @Test
    void getDefaultArmFreeIPAVmtypesShouldReturnMap() {
        Map<Region, List<String>> armTypes = Map.of(
                EU_CENTRAL_1, ARM_INSTANCE_TYPE,
                US_EAST_1, List.of("m6g.xlarge")
        );
        CloudRegions cloudRegions = createCloudRegions(armTypes,
                Map.of(
                        EU_CENTRAL_1, X86_INSTANCE_TYPE,
                        US_EAST_1, List.of("m5.xlarge")
                )
        );

        assertEquals(armTypes, cloudRegions.getDefaultArmFreeIPAVmtypes());
    }

    @Test
    void getDefaultX86FreeIPAVmtypesShouldReturnMap() {
        Map<Region, List<String>> x86Types = Map.of(EU_CENTRAL_1, X86_INSTANCE_TYPE, US_EAST_1, List.of("m5.xlarge"));
        CloudRegions cloudRegions = createCloudRegions(Map.of(EU_CENTRAL_1, ARM_INSTANCE_TYPE), x86Types);

        assertEquals(x86Types, cloudRegions.getDefaultX86FreeIPAVmtypes());
    }

    @Test
    void getDefaultArmFreeIPAVmtypesByRegionShouldReturnSetForRegion() {
        CloudRegions cloudRegions = createCloudRegions(
                Map.of(EU_CENTRAL_1, ARM_INSTANCE_TYPE),
                Map.of(EU_CENTRAL_1, X86_INSTANCE_TYPE));

        List<String> result = cloudRegions.getDefaultArmFreeIPAVmtypesByRegion("eu-central-1");

        assertEquals(ARM_INSTANCE_TYPE, result);
    }

    @Test
    void getDefaultX86FreeIPAVmtypesByRegionShouldReturnSetForRegion() {
        CloudRegions cloudRegions = createCloudRegions(
                Map.of(EU_CENTRAL_1, ARM_INSTANCE_TYPE),
                Map.of(EU_CENTRAL_1, X86_INSTANCE_TYPE)
        );

        List<String> result = cloudRegions.getDefaultX86FreeIPAVmtypesByRegion("eu-central-1");

        assertEquals(X86_INSTANCE_TYPE, result);
    }

    @Test
    void constructorShouldStoreAllFieldsCorrectly() {
        Map<Region, List<AvailabilityZone>> regions = Map.of(EU_CENTRAL_1, List.of(new AvailabilityZone("eu-central-1a")));
        Map<Region, String> displayNames = Map.of(EU_CENTRAL_1, "EU (Frankfurt)");
        Map<Region, Coordinate> coordinates = Map.of(EU_CENTRAL_1, Coordinate.defaultCoordinate());
        DefaultVmTypes vmTypes = new DefaultVmTypes(null, new ArchitectureVmTypes(X86_INSTANCE_TYPE, ARM_INSTANCE_TYPE));
        Map<Region, DefaultVmTypes> defaultVmtypesMap = Map.of(EU_CENTRAL_1, vmTypes);

        CloudRegions cloudRegions = new CloudRegions(regions, displayNames, coordinates, defaultVmtypesMap, "eu-central-1", true);

        assertEquals(regions, cloudRegions.getCloudRegions());
        assertEquals(displayNames, cloudRegions.getDisplayNames());
        assertEquals(coordinates, cloudRegions.getCoordinates());
        assertEquals(Map.of(EU_CENTRAL_1, ARM_INSTANCE_TYPE), cloudRegions.getDefaultArmFreeIPAVmtypes());
        assertEquals(Map.of(EU_CENTRAL_1, X86_INSTANCE_TYPE), cloudRegions.getDefaultX86FreeIPAVmtypes());
        assertEquals("eu-central-1", cloudRegions.getDefaultRegion());
        assertEquals(true, cloudRegions.areRegionsSupported());
    }

    private CloudRegions createCloudRegions(Map<Region, List<String>> armTypes, Map<Region, List<String>> x86Types) {
        Map<Region, DefaultVmTypes> vmtypesMap = new java.util.HashMap<>();
        for (Region r : armTypes.keySet()) {
            List<String> arm = armTypes.get(r);
            List<String> x86 = x86Types.get(r);
            vmtypesMap.put(r, new DefaultVmTypes(null, new ArchitectureVmTypes(x86, arm)));
        }
        for (Region r : x86Types.keySet()) {
            if (!vmtypesMap.containsKey(r)) {
                vmtypesMap.put(r, new DefaultVmTypes(null, new ArchitectureVmTypes(x86Types.get(r), null)));
            }
        }
        return new CloudRegions(
                Map.of(EU_CENTRAL_1, List.of()),
                Map.of(EU_CENTRAL_1, "EU (Frankfurt)"),
                Map.of(),
                vmtypesMap,
                "eu-central-1",
                true);
    }
}
