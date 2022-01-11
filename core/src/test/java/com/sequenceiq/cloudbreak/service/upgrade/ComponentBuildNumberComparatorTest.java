package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class ComponentBuildNumberComparatorTest {

    private static final String BUILD_NUMBER_KEY = "build-number";

    private final ComponentBuildNumberComparator underTest = new ComponentBuildNumberComparator();

    @Test
    public void testCompareShouldReturnFalseWhenTheCurrentIsGreaterThanTheNewBuildNumber() {
        Image currentImage = createImage("1234");
        Image newImage = createImage("1233");

        boolean actual = underTest.compare(currentImage, newImage, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    @Test
    public void testCompareShouldReturnTrueWhenTheCurrentIsLowerThanTheNewBuildNumber() {
        Image currentImage = createImage("1233");
        Image newImage = createImage("1234");

        boolean actual = underTest.compare(currentImage, newImage, BUILD_NUMBER_KEY);

        assertTrue(actual);
    }

    @Test
    public void testCompareShouldReturnTrueWhenTheCurrentIsEqualWithTheNewBuildNumber() {
        Image currentImage = createImage("1234");
        Image newImage = createImage("1234");

        boolean actual = underTest.compare(currentImage, newImage, BUILD_NUMBER_KEY);

        assertTrue(actual);
    }

    @Test
    public void testCompareShouldReturnFalseWhenTheCurrentBuildNumberIsNotPresent() {
        Image currentImage = createImage(null);
        Image newImage = createImage("1234");

        boolean actual = underTest.compare(currentImage, newImage, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    @Test
    public void testCompareShouldReturnFalseWhenTheNewBuildNumberIsNotPresent() {
        Image currentImage = createImage("1234");
        Image newImage = createImage(null);

        boolean actual = underTest.compare(currentImage, newImage, BUILD_NUMBER_KEY);

        assertFalse(actual);
    }

    private Image createImage(String buildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(BUILD_NUMBER_KEY, buildNumber);
        return new Image(null, null, null, null, null, null, null, null, null, null, null, packageVersions, null, null, null, true, null, null);
    }

}