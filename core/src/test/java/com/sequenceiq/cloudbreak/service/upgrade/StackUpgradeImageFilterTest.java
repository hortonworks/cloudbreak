package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.CompareLevel;
import com.sequenceiq.cloudbreak.cloud.CustomVersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.image.VersionBasedImageFilter;

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
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private CustomVersionComparator customVersionComparator;

    @Mock
    private VersionBasedImageFilter versionBasedImageFilter;

    @Mock
    private Versions supportedCbVersions;

    private Image currentImage;

    private Image properImage;

    @Before
    public void before() {
        currentImage = createCurrentImage();
        properImage = createProperImage();

        when(customVersionComparator.compare(V_7_0_2, V_7_0_3, CompareLevel.MINOR)).thenReturn(-1);

        when(customVersionComparator.compare(V_7_0_2, V_7_0_2, CompareLevel.MINOR)).thenReturn(0);
    }

    @Test
    public void testFilterShouldReturnTheAvailableImage() {
        List<Image> properImages = List.of(properImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, properImages)).thenReturn(properImages);

        Images actual = underTest.filter(properImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCloudPlatformIsNotMatches() {
        Image availableImages = createImageWithDifferentPlatform();
        List<Image> allImage = List.of(availableImages, properImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(allImage);

        Images actual = underTest.filter(allImage, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCmVersionIsNotGreater() {
        Image lowerCmImage = createImageWithLowerCmVersion();
        Image lowerCmAndCdpImage = createImageWithLowerCmAndCdpVersion();
        List<Image> availableImages = List.of(properImage, lowerCmImage, lowerCmAndCdpImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertTrue(actual.getCdhImages().contains(lowerCmImage));
        assertEquals(2, actual.getCdhImages().size());
    }

    @Test
    @Ignore("Extensions are not checked since they are not relevant from SDX point of view, anyway the comparator does work properly")
    public void testFilterShouldReturnTheProperImageWhenTheCmfVersionIsNotMatches() {
        // We use older CFM version
        Image differentCmfVersionImage = createImageWithCfmVersion("2.0.0.0-120");
        List<Image> availableImages = List.of(properImage, differentCmfVersionImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    @Ignore("Extensions are not checked since they are not relevant from SDX point of view, anyway the comparator does work properly")
    public void testFilterShouldReturnTheProperImageWhenTheCspVersionIsNotMatches() {
        Image differentCspVersionImage = createImageWithDifferentCspVersion();
        List<Image> availableImages = List.of(properImage, differentCspVersionImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheSaltVersionIsNotMatches() {
        Image differentSaltVersionImage = createImageWithDifferentSaltVersion();
        List<Image> availableImages = List.of(properImage, differentSaltVersionImage);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCurrentImageIsAlsoAvailable() {
        Image imageWithSameId = createImageWithCurrentImageId();
        List<Image> availableImages = List.of(properImage, imageWithSameId);

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(availableImages);

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnEmptyListWhenTheCurrentCbVersionIsNotSupported() {
        List<Image> availableImages = List.of(properImage);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(Collections.emptyList());

        Images actual = underTest.filter(availableImages, supportedCbVersions, currentImage, CLOUD_PLATFORM);

        assertTrue(actual.getCdhImages().isEmpty());
        verifyZeroInteractions(customVersionComparator);
    }

    private Image createCurrentImage() {
        return new Image(null, null, null, OS, CURRENT_IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private Image createProperImage() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private Image createImageWithDifferentPlatform() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null, Map.of("azure", IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private Image createImageWithLowerCmVersion() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                null, null, null);
    }

    private Image createImageWithLowerCmAndCdpVersion() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private Image createImageWithCfmVersion(String cfmVersion) {
        return new Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, cfmVersion, CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private Image createImageWithDifferentCspVersion() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, "4.0.0.0-103", SALT_VERSION), null, null, null);
    }

    private Image createImageWithDifferentSaltVersion() {
        return new Image(null, null, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, "2018.7.5"), null, null, null);
    }

    private Image createImageWithCurrentImageId() {
        return new Image(null, null, null, OS, CURRENT_IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP),
                null, OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION), null, null, null);
    }

    private Map<String, String> createPackageVersions(String cmVersion, String cdhVersion, String cfmVersion, String cspVersion, String saltVersion) {
        return Map.of(
                "cm", cmVersion,
                "stack", cdhVersion,
                "cfm", cfmVersion,
                "csp", cspVersion,
                "salt", saltVersion);
    }
}