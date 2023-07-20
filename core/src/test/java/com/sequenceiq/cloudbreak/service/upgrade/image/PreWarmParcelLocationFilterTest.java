package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class PreWarmParcelLocationFilterTest {

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/asdf/";

    private static final String RANDOM_URL = "http://random.cloudera.com/asdf/";

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private final PreWarmParcelLocationFilter underTest = new PreWarmParcelLocationFilter();

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackTypeIsNotWorkload() {
        assertTrue(underTest.filterImage(null, new ImageFilterParams(null, null, false, null, StackType.DATALAKE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsAreNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreEmpty() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ""), List.of("parcel2"));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenSomePreWarmParcelsListElementsAreNull() {
        List<List<String>> preWarmParcels = List.of(Arrays.asList("parcel1", ARCHIVE_URL), Arrays.asList(null, null));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreNull() {
        List<List<String>> preWarmParcels = List.of(Arrays.asList("parcel1", null), Arrays.asList(null, null));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThePreWarmParcelsAreEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsContainsCorruptedAndProperElementAsWell() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ARCHIVE_URL), Arrays.asList("parcel2", null));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThePreWarmParcelsAreNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", RANDOM_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenOnlyOneParcelIsNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, createImageFilterParams(Map.of("parcel1", "", "parcel2", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheImageDoesNotContainsPreWarmParcelFromTheStackRelatedParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("spark", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThereAreNoStackRelatedParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Collections.emptyMap())));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThereAreNoInstalledParcelInfoAvailable() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(null)));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheStackRelatedParcelsContainsOnlyTheIgnoredParcel() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("CDH", ""))));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThereAreNoPreWarmParcelAvailableForAllRequiresParcels() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, createImageFilterParams(Map.of("parcel", "", "spark", ""))));
    }

    private Image createImage(List<List<String>> preWarmParcels) {
        return new Image(null, null, null, null, null, null, null, null, null, null, null, null, preWarmParcels, null, null, true, null, null);
    }

    private ImageFilterParams createImageFilterParams(Map<String, String> stackRelatedParcels) {
        return new ImageFilterParams(null, null, false, stackRelatedParcels, StackType.WORKLOAD, null, STACK_ID, new InternalUpgradeSettings(false, true,
                true), imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false);
    }

}