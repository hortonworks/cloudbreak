package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;

class PreWarmParcelLocationFilterTest {

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/asdf/";

    private static final String RANDOM_URL = "http://random.cloudera.com/asdf/";

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private final PreWarmParcelLocationFilter underTest = new PreWarmParcelLocationFilter();

    @Test
    void testFilterImageShouldReturnTrueWhenTheStackTypeIsNotWorkload() {
        assertTrue(underTest.filterImage(null, new ImageFilterParams(null, null, null, false, false, null, StackType.DATALAKE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false)));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, createImageFilterParams(null)));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenImageIsArm64WithoutPrewarmedParcels() {
        Image image = createArmImage(List.of());
        assertTrue(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenThePreWarmParcelsAreNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreEmpty() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ""), List.of("parcel2"));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenSomePreWarmParcelsListElementsAreNull() {
        List<List<String>> preWarmParcels = List.of(Arrays.asList("parcel1", ARCHIVE_URL), Arrays.asList(null, null));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreNull() {
        List<List<String>> preWarmParcels = List.of(Arrays.asList("parcel1", null), Arrays.asList(null, null));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenThePreWarmParcelsAreEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel", ""))));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenThePreWarmParcelsContainsCorruptedAndProperElementAsWell() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ARCHIVE_URL), Arrays.asList("parcel2", null));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenThePreWarmParcelsAreNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", RANDOM_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnFalseWhenOnlyOneParcelIsNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenTheImageDoesNotContainsPreWarmParcelFromTheStackRelatedParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("spark", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenThereAreNoStackRelatedParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Collections.emptyMap())));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenThereAreNoInstalledParcelInfoAvailable() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsContainsOnlyTheIgnoredParcel() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("CDH", ""))));
    }

    @Test
    void testFilterImageShouldReturnTrueWhenThereAreNoPreWarmParcelAvailableForAllRequiresParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel", "", "spark", ""))));
    }

    private Image createImage(List<List<String>> preWarmParcels) {
        return Image.builder().withPreWarmParcels(preWarmParcels).build();
    }

    private Image createArmImage(List<List<String>> preWarmParcels) {
        return Image.builder().withPreWarmParcels(preWarmParcels).withArchitecture(Architecture.ARM64.getName()).build();
    }

    private ImageFilterParams createImageFilterParams(Map<String, String> stackRelatedParcels) {
        return new ImageFilterParams(null, null, null, false, false, stackRelatedParcels, StackType.WORKLOAD, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false);
    }

}