package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.service.image.VersionBasedImageFilter;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeImageFilterTest {

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

    private static final long CREATED = 100000L;

    private static final long CREATED_LARGER = 200000L;

    private static final String DATE = "2020-05-16";

    private static final String PARCEL_URL = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/3728680/csa/1.2.1.0/parcels";

    private static final String PARCEL_VERSION = "FLINK-1.10.0-csa1.2.1.0-cdh7.2.0.0-233-3728680-el7.parcel";

    private static final String PARCEL_NAME = "FLINK";

    @InjectMocks
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private VersionBasedImageFilter versionBasedImageFilter;

    @Mock
    private Versions supportedCbVersions;

    @Mock
    private EntitlementDrivenPackageLocationFilter entitlementDrivenPackageLocationFilter;

    @Mock
    private ImageCreationBasedFilter imageCreationBasedFilter;

    @Mock
    private CmAndStackVersionFilter cmAndStackVersionFilter;

    private Image currentImage;

    private Image properImage;

    private boolean lockComponents;

    private Map<String, String> activatedParcels;

    @Before
    public void before() {
        currentImage = createCurrentImage();
        properImage = createProperImage();
        activatedParcels = Map.of("stack", V_7_0_3);
        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.TRUE);
        when(entitlementDrivenPackageLocationFilter.filterImage(any(Image.class))).thenReturn(predicate);
        when(imageCreationBasedFilter.filterPreviousImages(any(Image.class), any())).thenReturn(predicate);
        when(cmAndStackVersionFilter.filterCmAndStackVersion(any(ImageFilterParams.class), any())).thenReturn(predicate);
    }

    @Test
    public void testFilterShouldReturnTheAvailableImage() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null), "");
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, properImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(properImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
    }

    @Test
    public void testFilterShouldNotReturnImageIfPackageFilterReturnFalse() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null), "");

        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.FALSE);
        when(entitlementDrivenPackageLocationFilter.filterImage(any(Image.class))).thenReturn(predicate);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, properImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(properImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
    }

    @Test
    public void testFilterShouldNotReturnImageIfCmAndStackFilterReturnFalse() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null), "");

        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.FALSE);
        when(cmAndStackVersionFilter.filterCmAndStackVersion(any(ImageFilterParams.class), any())).thenReturn(predicate);
        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, properImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(properImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCloudPlatformDoesNotMatches() {
        Image availableImages = createImageWithDifferentPlatform();
        List<Image> allImage = List.of(availableImages, properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(allImage, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheCloudPlatformIsNotMatches() {
        Image imageWithDifferentPlatform = createImageWithDifferentPlatform();
        List<Image> allImage = List.of(imageWithDifferentPlatform);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(allImage, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade for aws cloud platform.", actual.getReason());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheOsIsNotMatches() {
        Image imageWithDifferentOs = createImageWithDifferentOs();
        List<Image> allImage = List.of(imageWithDifferentOs);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(allImage, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheVersioningIsNotSupported() {
        List<Image> allImage = List.of(createImageWithDifferentStackVersioning());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(allImage, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images with supported Cloudera Manager or CDP version.", actual.getReason());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenCmVersionIsNotAvailable() {
        List<Image> allImage = List.of(createImageWithoutCmVersion());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, allImage)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(allImage, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade available with Cloudera Manager packages.", actual.getReason());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheStackVersionIsEqualAndTheBuildNumberIsGreater() {
        Image imageWithSameStackAndCmVersion = createImageWithSameStackAndCmVersion();
        List<Image> availableImages = List.of(imageWithSameStackAndCmVersion);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(availableImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(imageWithSameStackAndCmVersion));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCurrentImageIsAlsoAvailable() {
        Image imageWithSameId = createImageWithCurrentImageId();
        List<Image> availableImages = List.of(properImage, imageWithSameId);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(availableImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().contains(properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenOnlyTheCurrentImageIsAvailable() {
        List<Image> availableImages = List.of(createImageWithCurrentImageId());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(availableImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no newer compatible images available.", actual.getReason());
    }

    @Test
    public void testFilterShouldReturnOnlyOneImageWhenLockComponentsIsSetAndParcelIsActive() {
        Image imageWithSameStackAndCmVersion1 = createImageWithSameStackAndCmVersion();
        Image imageWithSameStackAndCmVersion2 = createImageWithSameStackAndCmVersion();
        Image imageWithDifferentStackVersioning = createImageWithDifferentStackVersioning();
        List<Image> availableImages = List.of(imageWithSameStackAndCmVersion1, imageWithSameStackAndCmVersion2, imageWithDifferentStackVersioning);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(PARCEL_NAME, PARCEL_VERSION), true);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null), "");

        when(versionBasedImageFilter.getCdhImagesForCbVersion(supportedCbVersions, availableImages)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(availableImages, supportedCbVersions, CLOUD_PLATFORM, imageFilterParams);

        assertEquals(actual.getReason(), 2, actual.getAvailableImages().getCdhImages().size());
    }

    private Image createCurrentImage() {
        return new Image(DATE, CREATED, null, OS, CURRENT_IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createProperImage() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createImageWithDifferentPlatform() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null, Map.of("azure", IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createImageWithDifferentOs() {
        return new Image(null, CREATED_LARGER, null, "ubuntu", IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createImageWithSameStackAndCmVersion() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createImageWithDifferentStackVersioning() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions("7.x.0", V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION),
                getParcels(), null, null);
    }

    private Image createImageWithoutCmVersion() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions("", V_7_0_2, CMF_VERSION, CSP_VERSION, SALT_VERSION), getParcels(), null, null);
    }

    private Image createImageWithCurrentImageId() {
        return new Image(null, CREATED_LARGER, null, OS, CURRENT_IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP),
                null, OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3, CMF_VERSION, CSP_VERSION, SALT_VERSION), getParcels(), null, null);
    }

    private Map<String, String> createPackageVersions(String cmVersion, String cdhVersion, String cfmVersion, String cspVersion, String saltVersion) {
        return Map.of(
                "cm", cmVersion,
                "stack", cdhVersion,
                "cfm", cfmVersion,
                "csp", cspVersion,
                "salt", saltVersion);
    }

    private List<List<String>> getParcels() {
        List<String> parcel = List.of(PARCEL_URL, PARCEL_VERSION);
        return Collections.singletonList(parcel);
    }

    private ClouderaManagerProduct getClouderaManagerProduct() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.withName(PARCEL_NAME).
                withVersion(PARCEL_VERSION).
                withParcel(PARCEL_URL);
        return product;
    }
}