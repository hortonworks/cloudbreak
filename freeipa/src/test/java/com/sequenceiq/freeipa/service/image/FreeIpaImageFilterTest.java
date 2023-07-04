package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@ExtendWith(MockitoExtension.class)
class FreeIpaImageFilterTest {

    private static final String REDHAT_8 = "redhat8";

    private static final String CENTOS_7 = "centos7";

    private static final String REGION_1 = "eu-central-1";

    private static final String AWS = "AWS";

    @InjectMocks
    private FreeIpaImageFilter underTest;

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Test
    void setUnderTestFilterImagesShouldReturnAllImageWhenMajorOsUpgradeIsEnabled() {
        List<Image> candidateImages = List.of(
                createImage("image-1", REDHAT_8, AWS, REGION_1),
                createImage("image-2", CENTOS_7, AWS, REGION_1)
        );
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, REGION_1, AWS, true);

        when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.containsAll(candidateImages));
        verify(providerSpecificImageFilter).filterImages(any(), any());
    }

    @Test
    void setUnderTestFilterImagesShouldReturnImageWithTheProperOsWhenMajorOsUpgradeIsNotEnabled() {
        Image image1 = createImage("image-1", REDHAT_8, AWS, REGION_1);
        Image image2 = createImage("image-2", CENTOS_7, AWS, REGION_1);
        List<Image> candidateImages = List.of(image1, image2);
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, REGION_1, AWS, false);

        when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

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
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, REGION_1, AWS, true);

        when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

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
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, REGION_1, AWS, false);

        when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

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
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(CENTOS_7, REGION_1, AWS, false);

        when(providerSpecificImageFilter.filterImages(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

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
        FreeIpaImageFilterSettings imageFilterSettings = createImageFilterSettings(REDHAT_8, "other-region", "azure", false);

        List<Image> actual = underTest.filterImages(candidateImages, imageFilterSettings);

        assertTrue(actual.containsAll(candidateImages));
        verifyNoInteractions(providerSpecificImageFilter);
    }

    private FreeIpaImageFilterSettings createImageFilterSettings(String os, String region, String platform, boolean allowMajorOsUpgrade) {
        return new FreeIpaImageFilterSettings(null, null, os, region, platform, allowMajorOsUpgrade);
    }

    private Image createImage(String imageId, String os, String platform, String region) {
        return new Image(null, null, null, os, imageId, Map.of(platform, Map.of(region, "imageName")), null, null, true);
    }

}