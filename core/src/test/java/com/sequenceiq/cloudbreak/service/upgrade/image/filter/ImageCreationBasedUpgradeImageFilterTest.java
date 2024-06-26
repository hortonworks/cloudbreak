package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class ImageCreationBasedUpgradeImageFilterTest {

    @InjectMocks
    private ImageCreationBasedUpgradeImageFilter underTest;

    @Mock
    private Image candidate;

    private com.sequenceiq.cloudbreak.cloud.model.Image current;

    @BeforeEach
    public void setUp() {
        lenient().when(candidate.getOs()).thenReturn("centos7");
        lenient().when(candidate.getOsType()).thenReturn("redhat7");
    }

    @Test
    public void testImageIsNewer() {
        current = createCurrentImage(1L);
        when(candidate.getCreated()).thenReturn(2L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageSameCreation() {
        current = createCurrentImage(1L);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageOlder() {
        current = createCurrentImage(2L);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testCurrentImageCreationNull() {
        current = createCurrentImage(null);
        when(candidate.getCreated()).thenReturn(1L);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testCandidateImageCreationNull() {
        current = createCurrentImage(1L);
        when(candidate.getCreated()).thenReturn(null);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    @Test
    public void testVersionDiffersOlder() {
        current = createCurrentImage(2L);
        mockDifferentVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testVersionDiffersNewer() {
        current = createCurrentImage(1L);
        mockDifferentVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testVersionDiffersSameOld() {
        current = createCurrentImage(1L);
        mockDifferentVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageIsNewerWithSameVersion() {
        current = createCurrentImage(1L);
        when(candidate.getCreated()).thenReturn(2L);
        mockSameVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageSameCreationWithSameVersion() {
        current = createCurrentImage(1L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testImageOlderWithSameVersion() {
        current = createCurrentImage(2L);
        when(candidate.getCreated()).thenReturn(1L);
        mockSameVersion();

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidate)), createImageFilterParams());

        assertTrue(actual.getImages().isEmpty());
        assertReason(actual.getReason());
    }

    private void mockSameVersion() {
        ImageStackDetails candidateStackDetails = mock(ImageStackDetails.class);
        when(candidateStackDetails.getVersion()).thenReturn("a");
        when(candidate.getStackDetails()).thenReturn(candidateStackDetails);
    }

    private void mockDifferentVersion() {
        ImageStackDetails candidateStackDetails = mock(ImageStackDetails.class);
        when(candidateStackDetails.getVersion()).thenReturn("b");
        when(candidate.getStackDetails()).thenReturn(candidateStackDetails);
    }

    private void assertReason(String reason) {
        assertTrue(reason.contains("There are no newer images available than"));
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(null, current, null, false, null, null, null, null, null, null, null, null, false);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage(Long created) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withOs("centos7")
                .withOsType("redhat7")
                .withPackageVersions(Map.of(ImagePackageVersion.STACK.getKey(), "a"))
                .withCreated(created)
                .build();
    }
}