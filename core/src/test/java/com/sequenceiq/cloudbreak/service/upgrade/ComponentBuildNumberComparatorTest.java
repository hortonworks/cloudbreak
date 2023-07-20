package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.util.StringUtils;

public class ComponentBuildNumberComparatorTest {

    private static final String BUILD_NUMBER_KEY = "build-number";

    private final ComponentBuildNumberComparator underTest = new ComponentBuildNumberComparator();

    @Test
    public void testCompareShouldReturnFalseWhenTheCurrentIsGreaterThanTheNewBuildNumber() {
        Map<String, String> currentImagePackages = createPackageVersions("1234");
        Map<String, String> newImagePackages = createPackageVersions("1233");

        boolean actual = underTest.compare(currentImagePackages, newImagePackages, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    @Test
    public void testCompareShouldReturnTrueWhenTheCurrentIsLowerThanTheNewBuildNumber() {
        Map<String, String> currentImagePackages = createPackageVersions("1233");
        Map<String, String> newImagePackages = createPackageVersions("1234");

        boolean actual = underTest.compare(currentImagePackages, newImagePackages, BUILD_NUMBER_KEY);

        assertTrue(actual);
    }

    @Test
    public void testCompareShouldReturnTrueWhenTheCurrentIsEqualWithTheNewBuildNumber() {
        Map<String, String> currentImagePackages = createPackageVersions("1234");
        Map<String, String> newImagePackages = createPackageVersions("1234");

        boolean actual = underTest.compare(currentImagePackages, newImagePackages, BUILD_NUMBER_KEY);

        assertTrue(actual);
    }

    @Test
    public void testCompareShouldReturnFalseWhenTheCurrentBuildNumberIsNotPresent() {
        Map<String, String> currentImagePackages = createPackageVersions(null);
        Map<String, String> newImagePackages = createPackageVersions("1234");

        boolean actual = underTest.compare(currentImagePackages, newImagePackages, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    @Test
    public void testCompareShouldReturnFalseWhenTheNewBuildNumberIsNotPresent() {
        Map<String, String> currentImagePackages = createPackageVersions("1234");
        Map<String, String> newImagePackages = createPackageVersions(null);

        boolean actual = underTest.compare(currentImagePackages, newImagePackages, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    private Map<String, String> createPackageVersions(String buildNumber) {
        return StringUtils.hasText(buildNumber) ? Map.of(BUILD_NUMBER_KEY, buildNumber) : Collections.emptyMap();
    }

}