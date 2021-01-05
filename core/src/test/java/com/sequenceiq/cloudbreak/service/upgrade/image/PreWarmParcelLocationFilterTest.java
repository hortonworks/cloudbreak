package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class PreWarmParcelLocationFilterTest {

    private static final StackType WORKLOAD_STACK_TYPE = StackType.WORKLOAD;

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/asdf/";

    private static final String RANDOM_URL = "http://random.cloudera.com/asdf/";

    private final PreWarmParcelLocationFilter underTest = new PreWarmParcelLocationFilter();

    @Test
    public void testFilterImageShouldReturnFalseWhenTheStackTypeIsNotWorkload() {
        assertFalse(underTest.filterImage(null, null, StackType.DATALAKE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsAreNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreEmpty() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ""), List.of("parcel2"));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsListElementsAreNull() {
        List<List<String>> preWarmParcels = List.of(Arrays.asList("parcel1", null), Arrays.asList(null, null));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThePreWarmParcelsAreEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenThePreWarmParcelsContainsCorruptedAndProperElementAsWell() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", ARCHIVE_URL), Arrays.asList("parcel2", null));
        Image image = createImage(preWarmParcels);
        assertTrue(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmParcelsAreNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel", RANDOM_URL));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenOnlyOneParcelIsNotEligibleForUpgrade() {
        List<List<String>> preWarmParcels = List.of(List.of("parcel1", RANDOM_URL), List.of("parcel2", ARCHIVE_URL));
        Image image = createImage(preWarmParcels);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    private Image createImage(List<List<String>> preWarmParcels) {
        return new Image(null, null, null, null, null, null, null, null, null, null, null, preWarmParcels, null, null);
    }

}