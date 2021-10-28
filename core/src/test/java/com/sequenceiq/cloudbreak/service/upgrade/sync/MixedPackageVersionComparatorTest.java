package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

public class MixedPackageVersionComparatorTest {

    private static final String CDH_KEY = "CDH";

    private static final String SPARK_KEY = "SPARK";

    private static final String SPARK_VERSION = "3.1.5";

    private static final String V_7_2_2 = "7.2.2";

    private final MixedPackageVersionComparator underTest = new MixedPackageVersionComparator();

    @Test
    void testMatchParcelVersionsShouldReturnTrueWhenAllParcelVersionsAreMatches() {
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> productsFromImage = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        assertTrue(underTest.matchParcelVersions(createParcelInfo(activeParcels), productsFromImage));
    }

    @Test
    void testMatchParcelVersionsShouldReturnFalseWhenCdhParcelVersionsAreNotMatches() {
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> productsFromImage = Map.of(CDH_KEY, "7.2.7", SPARK_KEY, SPARK_VERSION);
        assertFalse(underTest.matchParcelVersions(createParcelInfo(activeParcels), productsFromImage));
    }

    @Test
    void testMatchParcelVersionsShouldReturnFalseWhenTheActiveParcelsAreEmpty() {
        assertFalse(underTest.matchParcelVersions(Collections.emptySet(), Map.of(CDH_KEY, "7.2.7", SPARK_KEY, SPARK_VERSION)));
    }

    @Test
    void testMatchParcelVersionsShouldReturnFalseWhenTheProductsFromImageAreEmpty() {
        assertFalse(underTest.matchParcelVersions(createParcelInfo(Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION)), Collections.emptyMap()));
    }

    @Test
    void testFilterTargetPackageVersionsByNewerPackageVersionsShouldKeepOnlyTheNecessaryTargetVersion() {
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> newerComponentVersions = Map.of(CDH_KEY, "7.2.7", CM.getDisplayName(), "7.4.0");

        Map<String, String> actual = underTest.filterTargetPackageVersionsByNewerPackageVersions(targetProducts, V_7_2_2, newerComponentVersions);

        assertEquals(V_7_2_2, actual.get(CDH_KEY));
        assertEquals(V_7_2_2, actual.get(CM.getDisplayName()));
        assertEquals(2, actual.size());
    }

    @Test
    void testFilterTargetPackageVersionsByNewerPackageVersionsShouldKeepOnlyTheNecessaryTargetVersionWhenTheCmIsNotNewer() {
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> newerComponentVersions = Map.of(CDH_KEY, "7.2.7");

        Map<String, String> actual = underTest.filterTargetPackageVersionsByNewerPackageVersions(targetProducts, V_7_2_2, newerComponentVersions);

        assertEquals(V_7_2_2, actual.get(CDH_KEY));
        assertEquals(1, actual.size());
    }

    @Test
    void testGetComponentsWithNewerVersionThenTheTargetShouldReturnCdhVersion() {
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, "7.2.9", SPARK_KEY, SPARK_VERSION);

        Map<String, String> actual = underTest.getComponentsWithNewerVersionThanTheTarget(targetProducts, V_7_2_2, createParcelInfo(activeParcels), "7.2.0");

        assertEquals("7.2.9", actual.get(CDH_KEY));
        assertEquals(1, actual.size());
    }

    @Test
    void testGetComponentsWithNewerVersionThenTheTargetShouldReturnCdhAndCmVersion() {
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, "7.2.9", SPARK_KEY, SPARK_VERSION);
        String activeCmVersion = "7.4.0";

        Map<String, String> actual = underTest.getComponentsWithNewerVersionThanTheTarget(targetProducts, V_7_2_2, createParcelInfo(activeParcels),
                activeCmVersion);

        assertEquals("7.2.9", actual.get(CDH_KEY));
        assertEquals(activeCmVersion, actual.get(CM.getDisplayName()));
        assertEquals(2, actual.size());
    }

    @Test
    void testGetComponentsWithNewerVersionThenTheTargetShouldReturnEmptyMapWhenThereAreNoNewerVersion() {
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, "7.2.0", SPARK_KEY, SPARK_VERSION);

        Map<String, String> actual = underTest.getComponentsWithNewerVersionThanTheTarget(targetProducts, V_7_2_2, createParcelInfo(activeParcels), "7.2.0");

        assertTrue(actual.isEmpty());
    }

    @Test
    void testAreAllComponentVersionsAreMatchesWithImageShouldReturnTrueWhenAllComponentVersionMatches() {
        Map<String, String> productsFromImage = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        assertTrue(underTest.areAllComponentVersionsMatchingWithImage(V_7_2_2, productsFromImage, V_7_2_2, createParcelInfo(activeParcels)));
    }

    @Test
    void testAreAllComponentVersionsAreMatchesWithImageShouldReturnFalseWhenTheCmVersionNotMatches() {
        Map<String, String> productsFromImage = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        assertFalse(underTest.areAllComponentVersionsMatchingWithImage("7.2.0", productsFromImage, V_7_2_2, createParcelInfo(activeParcels)));
    }

    @Test
    void testAreAllComponentVersionsAreMatchesWithImageShouldReturnFalseWhenTheCdhVersionNotMatches() {
        Map<String, String> productsFromImage = Map.of(CDH_KEY, "7.2.0", SPARK_KEY, SPARK_VERSION);
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2, SPARK_KEY, SPARK_VERSION);
        assertFalse(underTest.areAllComponentVersionsMatchingWithImage(V_7_2_2, productsFromImage, V_7_2_2, createParcelInfo(activeParcels)));
    }

    private Set<ParcelInfo> createParcelInfo(Map<String, String> parcels) {
        return parcels.entrySet().stream().map(entry -> new ParcelInfo(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }
}