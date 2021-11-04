package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;

class ImageCreationBasedFilterTest {

    private final ImageCreationBasedFilter underTest = new ImageCreationBasedFilter();

    @Test
    public void testImageIsNewer() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testImageSameCreation() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testImageOlder() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertFalse(result);
        assertReason(reason);
    }

    @Test
    public void testCurrentImageCreationNull() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(null);
        when(candidate.getCreated()).thenReturn(1L);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertFalse(result);
        assertReason(reason);
    }

    @Test
    public void testCandidateImageCreationNull() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(null);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertFalse(result);
        assertReason(reason);
    }

    @Test
    public void testVersionDiffersOlder() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);
        mockDifferentVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testVersionDiffersNewer() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);
        mockDifferentVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testVersionDiffersSameOld() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);
        mockDifferentVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testImageIsNewerWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(2L);
        mockSameVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testImageSameCreationWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(1L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertTrue(result);
        assertReason(reason);
    }

    @Test
    public void testImageOlderWithSameVersion() {
        Image current = mock(Image.class);
        Image candidate = mock(Image.class);
        MutableObject<String> reason = new MutableObject<>();

        when(current.getCreated()).thenReturn(2L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion(current, candidate);

        Predicate<Image> predicate = underTest.filterPreviousImages(current, reason);
        boolean result = predicate.test(candidate);

        assertFalse(result);
        assertReason(reason);
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

    private void assertReason(MutableObject<String> reason) {
        assertTrue(reason.getValue().contains("There are no newer images available than"));
    }
}