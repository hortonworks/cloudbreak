package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;

public class CsdLocationFilterTest {

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/parcel1/";

    private static final String RANDOM_URL = "http://random.cloudera.com/parcel1/";

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private final CsdLocationFilter underTest = new CsdLocationFilter();

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackTypeIsNotWorkload() {
        assertTrue(underTest.filterImage(null, new ImageFilterParams(null, null, null, false, false, null, StackType.DATALAKE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenImageIsArm64WithoutPrewarmedParcels() {
        Image image = createArmImage(List.of());
        assertTrue(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdIsNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheCsdParcelsAreEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheCsdParcelsAreNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenOnlyOneCsdIsNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL, RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheImageDoesNotContainsCsdFromTheStackRelatedParcels() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("spark", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsAreNull() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsAreEmpty() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Collections.emptyMap())));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsContainsOnlyTheIgnoredParcel() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("CDH", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThereAreNoCsdAvailableForAllRequiresParcels() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "spark", ""))));
    }

    private Image createImage(List<String> preWarmCsd) {
        return Image.builder().withPreWarmCsd(preWarmCsd).build();
    }

    private Image createArmImage(List<String> preWarmCsd) {
        return Image.builder().withPreWarmCsd(preWarmCsd).withArchitecture(Architecture.ARM64.getName()).build();
    }

    private ImageFilterParams createImageFilterParams(Map<String, String> stackRelatedParcels) {
        return new ImageFilterParams(null, null, null, false, false, stackRelatedParcels, StackType.WORKLOAD, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false);
    }

}