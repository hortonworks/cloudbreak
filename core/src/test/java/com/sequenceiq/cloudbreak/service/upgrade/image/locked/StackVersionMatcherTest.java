package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;

class StackVersionMatcherTest {

    private static final Map<String, String> ACTIVATED_PARCELS = Map.of("PARCEL1", "PARCELVER1", "CDH", "CDHVER");

    private final Image image = mock(Image.class);

    private final StackVersionMatcher underTest = new StackVersionMatcher();

    @Test
    public void testMissingCdhVersion() {
        boolean result = underTest.isMatchingStackVersion(image, Map.of());

        assertTrue(result);
    }

    @Test
    public void testMissingStackDetails() {
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testMissingStackRepoDetails() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", null, "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testMissingStackInStackRepoDetails() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", new StackRepoDetails(null, null), "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testEmptyStackInStackRepoDetails() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", new StackRepoDetails(Map.of(), null), "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testUnrelatedStackInStackRepoDetails() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", new StackRepoDetails(Map.of("TEST", "DUMMY"), null), "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testVersionMatch() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", new StackRepoDetails(Map.of(REPOSITORY_VERSION, "CDHVER"), null), "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertTrue(result);
    }

    @Test
    public void testVersionNotMatch() {
        when(image.getStackDetails()).thenReturn(
                new ImageStackDetails("ver", new StackRepoDetails(Map.of(REPOSITORY_VERSION, "CDHDIFFERENTVER"), null), "build"));
        boolean result = underTest.isMatchingStackVersion(image, ACTIVATED_PARCELS);

        assertFalse(result);
    }

}