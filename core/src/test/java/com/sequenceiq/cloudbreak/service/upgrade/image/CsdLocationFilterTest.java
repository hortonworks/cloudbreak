package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class CsdLocationFilterTest {

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/parcel1/";

    private static final String RANDOM_URL = "http://random.cloudera.com/parcel1/";

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private final CsdLocationFilter underTest = new CsdLocationFilter();

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackTypeIsNotWorkload() {
        assertTrue(underTest.filterImage(null, null, new ImageFilterParams(null, false, null, StackType.DATALAKE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdIsNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheCsdParcelsAreEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheCsdParcelsAreNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, null, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenOnlyOneCsdIsNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL, RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, null, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheImageDoesNotContainsCsdFromTheStackRelatedParcels() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(Map.of("spark", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsAreNull() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsAreEmpty() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(Collections.emptyMap())));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsContainsOnlyTheIgnoredParcel() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(Map.of("CDH", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThereAreNoCsdAvailableForAllRequiresParcels() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, createImageFilterParams(Map.of("parcel1", "", "spark", ""))));
    }

    private Image createImage(List<String> preWarmCsd) {
        return new Image(null, null, null, null, null, null, null, null, null, null, null, null, null, preWarmCsd, null, true, null, null);
    }

    private ImageFilterParams createImageFilterParams(Map<String, String> stackRelatedParcels) {
        return new ImageFilterParams(null, false, stackRelatedParcels, StackType.WORKLOAD, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);
    }

}