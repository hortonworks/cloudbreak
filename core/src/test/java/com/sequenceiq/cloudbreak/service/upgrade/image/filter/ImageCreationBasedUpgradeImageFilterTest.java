package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

class ImageCreationBasedUpgradeImageFilterTest {

    private final ImageCreationBasedUpgradeImageFilter underTest = new ImageCreationBasedUpgradeImageFilter();

    @Test
    public void testImageIsNewer() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageSameCreation() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageOlder() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testCurrentImageCreationNull() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(null);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testCandidateImageCreationNull() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(null);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testVersionDiffersOlder() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);
        mockDifferentVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testVersionDiffersNewer() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);
        mockDifferentVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testVersionDiffersSameOld() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);
        mockDifferentVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageIsNewerWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);
        mockSameVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageSameCreationWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageOlderWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion(current, candidate);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams(current));

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    private void mockSameVersion(Image current, Image candidate) {
        ImageStackDetails currentStackDetails = mock(ImageStackDetails.class);
        ImageStackDetails candidateStackDetails = mock(ImageStackDetails.class);

        when(current.getStackDetails()).thenReturn(currentStackDetails);
        when(candidate.getStackDetails()).thenReturn(candidateStackDetails);
        when(currentStackDetails.getVersion()).thenReturn("a");
        when(candidateStackDetails.getVersion()).thenReturn("a");
    }

    private void mockDifferentVersion(Image current, Image candidate) {
        ImageStackDetails currentStackDetails = mock(ImageStackDetails.class);
        ImageStackDetails candidateStackDetails = mock(ImageStackDetails.class);

        when(current.getStackDetails()).thenReturn(currentStackDetails);
        when(candidate.getStackDetails()).thenReturn(candidateStackDetails);
        when(currentStackDetails.getVersion()).thenReturn("a");
        when(candidateStackDetails.getVersion()).thenReturn("b");
    }

    private void assertReason(String reason) {
        assertTrue(reason.contains("There are no newer images available than"));
    }

    private ImageFilterParams createImageFilterParams(Image currentImage) {
        return new ImageFilterParams(currentImage, false, null, null, null, null, null, null);
    }
}