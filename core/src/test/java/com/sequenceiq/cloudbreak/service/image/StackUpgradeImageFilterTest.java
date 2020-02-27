package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.CompareLevel;
import com.sequenceiq.cloudbreak.cloud.CustomVersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;

@RunWith(MockitoJUnitRunner.class)
public class StackUpgradeImageFilterTest {

    private static final String CLOUD_PLATFORM = "aws";

    private static final String OS_TYPE = "redhat7";

    private static final String OS = "centos7";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final String CMF_VERSION = "2.0.0.0-121";

    private static final String CSP_VERSION = "3.0.0.0-103";

    private static final String SALT_VERSION = "2017.7.5";

    private static final String CURRENT_IMAGE_ID = "f58c5f97-4609-4b47-6498-1c1bc6a4501c";

    private static final String IMAGE_ID = "f7f3fc53-b8a6-4152-70ac-7059ec8f8443";

    private static final Map<String, String> IMAGE_MAP = Collections.emptyMap();

    @InjectMocks
    private StackUpgradeImageFilter underTest;

    @Mock
    private CustomVersionComparator customVersionComparator;

    @Mock
    private VersionBasedImageFilter versionBasedImageFilter;

    @Mock
    private Versions supportedCbVersions;

    private Image currentImage;

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image properImage;

    @Before
    public void before() {
        currentImage = createCurrentImage();
        properImage = createProperImage();
    }

    @Test
    public void testFilterShouldReturnTheAvailableImage() {
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> properImage = List.of(this.properImage);
        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, properImage)).thenReturn(properImage);

        Images actual = underTest.filter(properImage, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCloudPlatformIsNotMatches() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image availableImages = createImageWithDifferentPlatform();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> allImage = List.of(availableImages, properImage);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(allImage);

        Images actual = underTest.filter(allImage, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCmVersionIsNotGreater() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image lowerCmImage = createImageWithLowerCmVersion();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image lowerCmAndCdpImage = createImageWithLowerCmAndCdpVersion();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(properImage, lowerCmImage, lowerCmAndCdpImage);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(customVersionComparator.compare(V_7_0_2, V_7_0_2, CompareLevel.MAINTENANCE)).thenReturn(0);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertTrue(actual.getCdhImages().contains(lowerCmImage));
        assertEquals(2, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCmfVersionIsNotMatches() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image differentCmfVersionImage = createImageWithDifferentCmfVersion();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(properImage, differentCmfVersionImage);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCspVersionIsNotMatches() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image differentCspVersionImage = createImageWithDifferentCspVersion();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(properImage, differentCspVersionImage);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheSaltVersionIsNotMatches() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image differentSaltVersionImage = createImageWithDifferentSaltVersion();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(properImage, differentSaltVersionImage);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCurrentImageIsAlsoAvailable() {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image imageWithSameId = createImageWithCurrentImageId();
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(properImage, imageWithSameId);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MAINTENANCE)).thenReturn(-1);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnEmptyListWhenTheCurrentCbVersionIsNotSupported() {
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> availableImages = List.of(this.properImage);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(Collections.emptyList());

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().isEmpty());
        verifyZeroInteractions(customVersionComparator);
    }

    private Image createCurrentImage() {
        return new Image(null, null, OS, OS_TYPE, null, null, CURRENT_IMAGE_ID,
                createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION));
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createProperImage() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithDifferentPlatform() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null, Map.of("azure", IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithLowerCmVersion() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithLowerCmAndCdpVersion() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithDifferentCmfVersion() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, "3.0.0.0-121", CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithDifferentCspVersion() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, "4.0.0.0-103", SALT_VERSION), null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithDifferentSaltVersion() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, "2018.7.5"), null, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createImageWithCurrentImageId() {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, OS, CURRENT_IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP),
                null, OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private Map<String, String> createPackageVersions(String cmVersion, String cdhVersion, String cmfVersion, String cspVersion, String saltVersion) {
        return Map.of(
                "cm", cmVersion,
                "stack", cdhVersion,
                "cfm", cmfVersion,
                "csp", cspVersion,
                "salt", saltVersion);
    }
}