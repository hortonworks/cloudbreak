package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;

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

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private static final String CATALOG_NAME = "catalog name";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private EntitlementDrivenPackageLocationFilter entitlementDrivenPackageLocationFilter;

    @Mock
    private ImageCreationBasedFilter imageCreationBasedFilter;

    @Mock
    private CmAndStackVersionFilter cmAndStackVersionFilter;

    @Mock
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Mock
    private CloudbreakImageCatalogV3 cloudbreakImageCatalogV3;

    @Mock
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageCatalogService imageCatalogService;

    private Image currentImage;

    private Image properImage;

    private Blueprint blueprint;

    private boolean lockComponents;

    private Map<String, String> activatedParcels;

    private String accountId = "account1";

    @Before
    public void before() {
        blueprint = new Blueprint();
        blueprint.setName("7.X.X - SDX template with Atlas, HMS, Ranger and other services they are dependent on");
        currentImage = createCurrentImage();
        properImage = createProperImage();
        activatedParcels = Map.of("stack", V_7_0_3);
        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.TRUE);
        when(entitlementDrivenPackageLocationFilter.filterImage(any(Image.class), any(ImageFilterParams.class))).thenReturn(predicate);
        when(imageCreationBasedFilter.filterPreviousImages(any(Image.class), any())).thenReturn(predicate);
        when(cmAndStackVersionFilter.filterCmAndStackVersion(any(ImageFilterParams.class), any())).thenReturn(predicate);
    }

    @Test
    public void testFilterShouldReturnTheAvailableImage() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null, null), "");
        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnTheAvailableImageWhenTheStackTypeIsWorkload() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.WORKLOAD, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null, null), "");
        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(blueprint, imageFilterParams.isLockComponents())).thenReturn(true);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(blueprint, imageFilterParams.isLockComponents());
    }

    @Test
    public void testFilterShouldNotReturnTheAvailableImageWhenTheBlueprintIsNotEligibleForUpgradeAndTheStackTypeIsWorkload() {
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.WORKLOAD, blueprint);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(blueprint, imageFilterParams.isLockComponents())).thenReturn(false);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertEquals("The upgrade is not allowed for this template.", actual.getReason());
        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(blueprint, imageFilterParams.isLockComponents());
        verifyNoInteractions(imageCatalogServiceProxy);
    }

    @Test
    public void testFilterShouldNotReturnImageIfPackageFilterReturnFalse() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null, null), "");

        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.FALSE);
        when(entitlementDrivenPackageLocationFilter.filterImage(any(Image.class), eq(imageFilterParams))).thenReturn(predicate);
        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldNotReturnImageIfCmAndStackFilterReturnFalse() {
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null, null), "");

        Predicate<Image> predicate = mock(Predicate.class);
        when(predicate.test(any())).thenReturn(Boolean.FALSE);
        when(cmAndStackVersionFilter.filterCmAndStackVersion(any(ImageFilterParams.class), any())).thenReturn(predicate);
        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCloudPlatformDoesNotMatches() {
        Image availableImages = createImageWithDifferentPlatform();
        List<Image> allImage = List.of(availableImages, properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheCloudPlatformIsNotMatches() {
        Image imageWithDifferentPlatform = createImageWithDifferentPlatform();
        List<Image> allImage = List.of(imageWithDifferentPlatform);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade for aws cloud platform.", actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheOsIsNotMatches() {
        Image imageWithDifferentOs = createImageWithDifferentOs();
        List<Image> allImage = List.of(imageWithDifferentOs);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheVersioningIsNotSupported() {
        List<Image> allImage = List.of(createImageWithDifferentStackVersioning());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images with supported Cloudera Manager or CDP version.", actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenCmVersionIsNotAvailable() {
        List<Image> allImage = List.of(createImageWithoutCmVersion());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, allImage, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no eligible images to upgrade available with Cloudera Manager packages.", actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenTheStackVersionIsEqualAndTheBuildNumberIsGreater() {
        Image imageWithSameStackAndCmVersion = createImageWithSameStackAndCmVersion();
        List<Image> availableImages = List.of(imageWithSameStackAndCmVersion);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(imageWithSameStackAndCmVersion));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnTheProperImageWhenTheCurrentImageIsAlsoAvailable() {
        Image imageWithSameId = createImageWithCurrentImageId();
        List<Image> availableImages = List.of(properImage, imageWithSameId);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().contains(properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnReasonMessageWhenOnlyTheCurrentImageIsAvailable() {
        List<Image> availableImages = List.of(createImageWithCurrentImageId());
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        assertEquals("There are no newer compatible images available.", actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldReturnOnlyOneImageWhenLockComponentsIsSetAndParcelIsActive() {
        Image imageWithSameStackAndCmVersion1 = createImageWithSameStackAndCmVersion();
        Image imageWithSameStackAndCmVersion2 = createImageWithSameStackAndCmVersion();
        Image imageWithDifferentStackVersioning = createImageWithDifferentStackVersioning();
        List<Image> availableImages = List.of(imageWithSameStackAndCmVersion1, imageWithSameStackAndCmVersion2, imageWithDifferentStackVersioning);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(PARCEL_NAME, PARCEL_VERSION), DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertEquals(actual.getReason(), 2, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void advertisedImageFilterShouldBeCalledInCaseOfNullVersions() {
        Image imageWithSameStackAndCmVersion1 = createImageWithSameStackAndCmVersion();
        Image imageWithSameStackAndCmVersion2 = createImageWithSameStackAndCmVersion();
        Image imageWithDifferentStackVersioning = createImageWithDifferentStackVersioning();
        List<Image> availableImages = List.of(imageWithSameStackAndCmVersion1, imageWithSameStackAndCmVersion2, imageWithDifferentStackVersioning);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(PARCEL_NAME, PARCEL_VERSION), DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, availableImages, null, null), "");

        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertEquals(actual.getReason(), 2, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testNotAllowedUpgradeMediumDuty() {
        when(entitlementService.haUpgradeEnabled(accountId)).thenReturn(false);
        blueprint = new Blueprint();
        blueprint.setName("SDX Medium Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.DATALAKE, blueprint);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertEquals("The upgrade is not allowed for this template.", actual.getReason());
        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        verifyNoInteractions(imageCatalogServiceProxy);
    }

    @Test
    public void testAllowedUpgradeMediumDuty() {
        when(entitlementService.haUpgradeEnabled(accountId)).thenReturn(true);
        blueprint = new Blueprint();
        blueprint.setName("SDX Medium Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas.");
        List<Image> properImages = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, DATALAKE_STACK_TYPE, blueprint);
        ImageFilterResult imageFilterResult = new ImageFilterResult(new Images(null, properImages, null, null), "");
        when(imageCatalogServiceProxy.getImageFilterResult(cloudbreakImageCatalogV3)).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(accountId, cloudbreakImageCatalogV3, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
    }

    @Test
    public void testFilterShouldNotReturnTheAvailableImageByImageCatalogNameWhenTheBlueprintIsNotEligibleForUpgrade() {
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.WORKLOAD, blueprint);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(blueprint)).thenReturn(false);

        ImageFilterResult actual = underTest.filter(accountId, WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM, imageFilterParams);

        assertEquals("The upgrade is not allowed for this template.", actual.getReason());
        assertTrue(actual.getAvailableImages().getCdhImages().isEmpty());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(blueprint);
        verifyNoInteractions(imageCatalogService);
    }

    @Test
    public void testFilterShouldReturnTheAvailableImageByImageCatalogName() throws CloudbreakImageCatalogException {
        List<Image> imageForUpgrade = List.of(properImage);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.WORKLOAD, blueprint);
        when(imageCatalogService.getImages(WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM))
                .thenReturn(StatedImages.statedImages(new Images(null, imageForUpgrade, null, null), null, null));
        when(blueprintUpgradeOptionValidator.isValidBlueprint(blueprint)).thenReturn(true);

        ImageFilterResult actual = underTest.filter(accountId, WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM, imageFilterParams);

        assertTrue(actual.getReason(), actual.getAvailableImages().getCdhImages().contains(this.properImage));
        assertEquals(1, actual.getAvailableImages().getCdhImages().size());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(blueprint);
    }

    private Image createCurrentImage() {
        return new Image(DATE, CREATED, null, OS, CURRENT_IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_2, V_7_0_2),
                getParcels(), null, null, true, null, null);
    }

    private Image createProperImage() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3),
                getParcels(), null, null, true, null, null);
    }

    private Image createImageWithDifferentPlatform() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null, Map.of("azure", IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3),
                getParcels(), null, null, true, null, null);
    }

    private Image createImageWithDifferentOs() {
        return new Image(null, CREATED_LARGER, null, "ubuntu", IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE,
                createPackageVersions(V_7_0_3, V_7_0_3),
                getParcels(), null, null, true, null, null);
    }

    private Image createImageWithSameStackAndCmVersion() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, IMAGE_MAP), null, OS_TYPE, createPackageVersions(V_7_0_2, V_7_0_2),
                getParcels(), null, null, true, null, null);
    }

    private Image createImageWithDifferentStackVersioning() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null,
                Map.of(CLOUD_PLATFORM, Collections.emptyMap()), null, OS_TYPE,
                createPackageVersions("7.x.0", V_7_0_3),
                getParcels(), null, null, true, null, null);
    }

    private Image createImageWithoutCmVersion() {
        return new Image(null, CREATED_LARGER, null, OS, IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP), null,
                OS_TYPE, createPackageVersions("", V_7_0_2), getParcels(), null, null, true, null, null);
    }

    private Image createImageWithCurrentImageId() {
        return new Image(null, CREATED_LARGER, null, OS, CURRENT_IMAGE_ID, null, null, Map.of(CLOUD_PLATFORM, IMAGE_MAP),
                null, OS_TYPE, createPackageVersions(V_7_0_3, V_7_0_3), getParcels(), null, null, true, null, null);
    }

    private Map<String, String> createPackageVersions(String cmVersion, String cdhVersion) {
        return Map.of(
                "cm", cmVersion,
                "stack", cdhVersion,
                "cfm", ClusterUpgradeImageFilterTest.CMF_VERSION,
                "csp", ClusterUpgradeImageFilterTest.CSP_VERSION,
                "salt", ClusterUpgradeImageFilterTest.SALT_VERSION);
    }

    private List<List<String>> getParcels() {
        List<String> parcel = List.of(PARCEL_URL, PARCEL_VERSION);
        return Collections.singletonList(parcel);
    }
}