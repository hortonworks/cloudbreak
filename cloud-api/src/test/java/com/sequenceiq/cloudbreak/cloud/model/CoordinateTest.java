package com.sequenceiq.cloudbreak.cloud.model;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.domain.CdpSupportedServices;

class CoordinateTest {

    @Test
    void coordinateFactoryShouldSetFreeIPAVmTypes() {
        DefaultVmTypes vmTypes = new DefaultVmTypes(
                new ArchitectureVmTypes(List.of("db.m5.large"), List.of("db.m6g.large")),
                new ArchitectureVmTypes(List.of("m5.2xlarge"), List.of("m6g.2xlarge"))
        );
        Coordinate coord = coordinate(
                "10.0", "20.0",
                "EU (Frankfurt)", "eu-central-1",
                true,
                List.of("ENTITLEMENT_1"),
                vmTypes,
                Set.of(CdpSupportedServices.ALL));

        assertEquals(List.of("m5.2xlarge"), coord.getDefaultX86FreeIPAVmtypes());
        assertEquals(List.of("m6g.2xlarge"), coord.getDefaultArmFreeIPAVmtypes());
    }

    @Test
    void coordinateFactoryShouldSetAllFields() {
        DefaultVmTypes vmTypes = new DefaultVmTypes(
                new ArchitectureVmTypes(List.of("db.m5.large"), List.of("db.m6g.large")),
                new ArchitectureVmTypes(List.of("m5.xlarge"), List.of("m6g.xlarge"))
        );
        Coordinate coord = coordinate(
                "10.5", "20.3",
                "US East (N. Virginia)", "us-east-1",
                false,
                List.of("ENT1", "ENT2"),
                vmTypes,
                Set.of(CdpSupportedServices.ALL));

        assertEquals(10.5, coord.getLongitude());
        assertEquals(20.3, coord.getLatitude());
        assertEquals("US East (N. Virginia)", coord.getDisplayName());
        assertEquals("us-east-1", coord.getKey());
        assertEquals(false, coord.getK8sSupported());
        assertEquals(List.of("ENT1", "ENT2"), coord.getEntitlements());
        assertEquals(List.of("db.m5.large"), coord.getDefaultDbVmTypes());
        assertEquals(List.of("db.m6g.large"), coord.getDefaultArmDbVmTypes());
        assertEquals(List.of("m5.xlarge"), coord.getDefaultX86FreeIPAVmtypes());
        assertEquals(List.of("m6g.xlarge"), coord.getDefaultArmFreeIPAVmtypes());
        assertEquals(Set.of(CdpSupportedServices.ALL), coord.getCdpSupportedServices());
    }

    @Test
    void coordinateFactoryShouldHandleNullCdpSupportedServices() {
        Coordinate coord = coordinate(
                "10.0", "20.0",
                "display", "key",
                true,
                List.of(),
                new DefaultVmTypes(),
                null);

        assertTrue(coord.getCdpSupportedServices().isEmpty());
    }

    @Test
    void defaultCoordinateShouldHaveNullFreeIPAVmTypes() {
        Coordinate coord = Coordinate.defaultCoordinate();

        assertNull(coord.getDefaultX86FreeIPAVmtypes());
        assertNull(coord.getDefaultArmFreeIPAVmtypes());
        assertNull(coord.getDefaultDbVmTypes());
        assertNull(coord.getDefaultArmDbVmTypes());
    }

    @Test
    void isMatchedRegionShouldMatchByKey() {
        Coordinate coord = coordinate(
                "10.0", "20.0",
                "EU (Frankfurt)", "eu-central-1",
                true, List.of(),
                new DefaultVmTypes(),
                Set.of());

        assertTrue(coord.isMatchedRegion(Region.region("eu-central-1")));
    }

    @Test
    void isMatchedRegionShouldMatchByDisplayName() {
        Coordinate coord = coordinate(
                "10.0", "20.0",
                "EU (Frankfurt)", "eu-central-1",
                true, List.of(),
                new DefaultVmTypes(),
                Set.of());

        assertTrue(coord.isMatchedRegion(Region.region("EU (Frankfurt)")));
    }
}
