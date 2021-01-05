package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class CsdLocationFilterTest {

    private static final StackType WORKLOAD_STACK_TYPE = StackType.WORKLOAD;

    private static final String ARCHIVE_URL = "http://archive.cloudera.com/asdf/";

    private static final String RANDOM_URL = "http://random.cloudera.com/asdf/";

    private final CsdLocationFilter underTest = new CsdLocationFilter();

    @Test
    public void testFilterImageShouldReturnFalseWhenTheStackTypeIsNotWorkload() {
        assertFalse(underTest.filterImage(null, null, StackType.DATALAKE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheImageIsNull() {
        assertFalse(underTest.filterImage(null, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdIsNull() {
        Image image = createImage(null);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenThePreWarmCsdListIsEmpty() {
        Image image = createImage(Collections.emptyList());
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnTrueWhenTheCsdParcelsAreEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL);
        Image image = createImage(preWarmCsd);
        assertTrue(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenTheCsdParcelsAreNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    @Test
    public void testFilterImageShouldReturnFalseWhenOnlyOneCsdIsNotEligibleForUpgrade() {
        List<String> preWarmCsd = List.of(ARCHIVE_URL, RANDOM_URL);
        Image image = createImage(preWarmCsd);
        assertFalse(underTest.filterImage(image, null, WORKLOAD_STACK_TYPE));
    }

    private Image createImage(List<String> preWarmCsd) {
        return new Image(null, null, null, null, null, null, null, null, null, null, null, null, preWarmCsd, null);
    }

}