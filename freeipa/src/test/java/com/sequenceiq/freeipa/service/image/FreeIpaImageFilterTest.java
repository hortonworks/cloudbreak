package com.sequenceiq.freeipa.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@ExtendWith(MockitoExtension.class)
class FreeIpaImageFilterTest {

    private static final String REDHAT_8 = "redhat8";

    private static final String REDHAT_9 = "redhat9";

    private static final String CENTOS_7 = "centos7";

    private static final String REGION_1 = "eu-central-1";

    private static final String AWS = "AWS";

    @InjectMocks
    private FreeIpaImageFilter underTest;

    @Mock
    private SupportedOsService supportedOsService;

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Mock
    private PreferredOsService preferredOsService;

    @BeforeEach
    void setUp() {
        lenient().when(supportedOsService.isSupported(any())).thenReturn(true);
        lenient().when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void findMostRecentImagePrefersLatestCreatedRegardlessOfOrder() {
        when(preferredOsService.getDefaultOs()).thenReturn(REDHAT_8);
        Image older = createDatedImage("image-z", "2021-09-01", 100L, REDHAT_8);
        Image newer = createDatedImage("image-a", "2021-09-01", 200L, REDHAT_8);

        assertThat(underTest.findMostRecentImage(List.of(older, newer))).contains(newer);
        assertThat(underTest.findMostRecentImage(List.of(newer, older))).contains(newer);
    }

    @Test
    void findMostRecentImageBreaksCreatedTieByUuidRegardlessOfOrder() {
        when(preferredOsService.getDefaultOs()).thenReturn(REDHAT_8);
        Image first = createDatedImage("image-a", "2021-09-01", 200L, REDHAT_8);
        Image last = createDatedImage("image-z", "2021-09-01", 200L, REDHAT_8);

        assertThat(underTest.findMostRecentImage(List.of(first, last))).contains(last);
        assertThat(underTest.findMostRecentImage(List.of(last, first))).contains(last);
    }

    @Test
    void findMostRecentImagePreservesPreferredOsAndDatePriority() {
        when(preferredOsService.getDefaultOs()).thenReturn(REDHAT_8);
        Image otherOs = createDatedImage("image-z", "2021-10-01", 300L, CENTOS_7);
        Image olderDate = createDatedImage("image-y", "2021-08-01", 200L, REDHAT_8);
        Image preferred = createDatedImage("image-a", "2021-09-01", 100L, REDHAT_8);

        assertThat(underTest.findMostRecentImage(List.of(otherOs, olderDate, preferred))).contains(preferred);
    }

    @Test
    void setUnderTestFilterImagesShouldReturnAllImageWhenMajorOsUpgradeIsEnabled() {
        List<Image> candidateImages = List.of(
                createImage("image-1", REDHAT_8, AWS, REGION_1),
                createImage("image-2", CENTOS_7, AWS, REGION_1)
        );
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, null, REGION_1, AWS, true);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.containsAll(candidateImages));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnAllButRhel9ImageWhenMajorOsUpgradeIsEnabled() {
        List<Image> candidateImages = List.of(
                createImage("image-1", REDHAT_8, AWS, REGION_1),
                createImage("image-3", REDHAT_9, AWS, REGION_1),
                createImage("image-2", CENTOS_7, AWS, REGION_1)
        );
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, null, REGION_1, AWS, true);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.containsAll(List.of(createImage("image-1", REDHAT_8, AWS, REGION_1),
                createImage("image-2", CENTOS_7, AWS, REGION_1))));
        assertEquals(2, actual.size());
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnImageWithTheProperOsWhenMajorOsUpgradeIsNotEnabled() {
        Image image1 = createImage("image-1", REDHAT_8, AWS, REGION_1);
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, null, REGION_1, AWS, false);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.contains(image2));
        assertFalse(actual.contains(image1));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnImageWithTheProperOsWhenMajorOsUpgradeIsEnabled() {
        Image image1 = createImage("image-1", "amazonlinux", AWS, REGION_1);
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, null, REGION_1, AWS, true);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.contains(image2));
        assertFalse(actual.contains(image1));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnImageWithTheProperPlatform() {
        Image image1 = createImage("image-1", CENTOS_7, "azure", REGION_1);
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, null, REGION_1, AWS, false);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.contains(image2));
        assertFalse(actual.contains(image1));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnImageWithTheProperRegion() {
        Image image1 = createImage("image-1", CENTOS_7, AWS, "other-region");
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, CENTOS_7, REGION_1, AWS, false);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.contains(image2));
        assertFalse(actual.contains(image1));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnCandidateImagesWhenNoProperImageFound() {
        Image image1 = createImage("image-1", CENTOS_7, AWS, REGION_1);
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(REDHAT_8, null, "other-region", "azure", false);

        Assertions.assertThatThrownBy(() -> underTest.filterImages(candidateImages, imageFilterSettings))
                .isInstanceOf(ImageNotFoundException.class)
                .hasMessageStartingWith("Could not find any FreeIPA image");
    }

    @Test
    void testGetImageWithoutOsRedhat8() {
        Image image1 = createImage("image-1", CENTOS_7, AWS, REGION_1);
        Image image2 = createImage("image-2", REDHAT_8, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);

        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(REDHAT_8, REDHAT_8, REGION_1, AWS, false);

        Optional<Image> image = underTest.filterImages(candidateImages, imageFilterSettings).stream().findFirst();

        assertTrue(image.isPresent());
        assertEquals(REDHAT_8, image.get().getOs());
        assertEquals("image-2", image.get().getUuid());
    }

    @Test
    void testFilterImagesWhereSelinuxNotSupported() {
        Image image1 = createImage("image-1", REDHAT_8, AWS, REGION_1, Map.of("selinux-supported", "false", "other-tag", "value"));
        Image image2 = createImage("image-2", REDHAT_8, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);

        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(REDHAT_8, null, REGION_1, AWS, false,
                Map.of("selinux-supported", "true"));

        List<Image> result = underTest.filterImages(candidateImages, imageFilterSettings);

        assertThat(result).hasSize(1);
        assertEquals("image-2", result.getFirst().getUuid());
    }

    private FreeIpaImageFilterSettings createImageFilterSettings(String currentOs, String targetOs, String region, String platform,
            boolean allowMajorOsUpgrade) {
        return createImageFilterSettings(currentOs, targetOs, region, platform, allowMajorOsUpgrade, Map.of());
    }

    private FreeIpaImageFilterSettings createImageFilterSettings(String currentOs, String targetOs, String region, String platform,
            boolean allowMajorOsUpgrade, Map<String, String> tagFilters) {
        return new FreeIpaImageFilterSettings(null, null, currentOs,  targetOs, region, platform, allowMajorOsUpgrade, Architecture.X86_64, tagFilters);
    }

    @Test
    void filterImagesShouldMatchBySourceImageIdWhenFlagIsEnabled() {
        String currentImageId = "3b945b5d-1a47-412d-a438-8a7da6e73cad";
        String customImageId = "a1804635-b1cb-4cd4-93c6-c44fb57e0a14";
        Image customImage = createImageWithSourceImageId(customImageId, REDHAT_8, AWS, REGION_1, currentImageId);
        Image unrelatedImage = createImage("other-image", REDHAT_8, AWS, REGION_1);
        List<Image> candidateImages = List.of(customImage, unrelatedImage);
        FreeIpaImageFilterSettings imageFilterSettings = new FreeIpaImageFilterSettings(currentImageId, null, REDHAT_8, REDHAT_8, REGION_1, AWS, false,
                Architecture.X86_64, Map.of(), true);

        List<Image> result = underTest.filterImages(candidateImages, imageFilterSettings);

        assertThat(result).containsExactly(customImage);
    }

    @Test
    void filterImagesShouldNotMatchBySourceImageIdWhenFlagIsDisabled() {
        String currentImageId = "3b945b5d-1a47-412d-a438-8a7da6e73cad";
        String customImageId = "a1804635-b1cb-4cd4-93c6-c44fb57e0a14";
        Image customImage = createImageWithSourceImageId(customImageId, REDHAT_8, AWS, REGION_1, currentImageId);
        List<Image> candidateImages = List.of(customImage);
        FreeIpaImageFilterSettings imageFilterSettings = new FreeIpaImageFilterSettings(currentImageId, null, REDHAT_8, REDHAT_8, REGION_1, AWS, false,
                Architecture.X86_64);

        List<Image> result = underTest.filterImages(candidateImages, imageFilterSettings);

        assertThat(result).isEmpty();
    }

    private Image createImage(String imageId, String os, String platform, String region) {
        return createImage(imageId, os, platform, region, Map.of());
    }

    private Image createImage(String imageId, String os, String platform, String region, Map<String, String> tags) {
        return new Image(null, null, null, os, imageId, Map.of(platform, Map.of(region, "imageName")), null, null, true, "x86_64", tags);
    }

    private Image createImageWithSourceImageId(String imageId, String os, String platform, String region, String sourceImageId) {
        return new Image(null, null, null, os, imageId, Map.of(platform, Map.of(region, "imageName")), null, null, true, "x86_64", Map.of(), sourceImageId);
    }

    private Image createDatedImage(String uuid, String date, long created, String os) {
        return new Image(created, date, null, os, uuid, Map.of(), null, Map.of(), true, "x86_64", Map.of());
    }
}
