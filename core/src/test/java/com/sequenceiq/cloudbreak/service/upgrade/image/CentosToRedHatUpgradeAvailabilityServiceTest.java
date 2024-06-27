package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CFM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfoFactory;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class CentosToRedHatUpgradeAvailabilityServiceTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String CM_BUILD_NUMBER = "12345";

    private static final Set<String> STACK_RELATED_PARCELS = Set.of(CDH.name(), CFM.getKey());

    private static final String TARGET_IMAGE_ID = "target-image";

    private static final long STACK_ID = 1L;

    private static final String CDH_VERSION = "7.2.17-12345";

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String PLATFORM_VARIANT = "platform-variant";

    private static final String ACCOUNT_ID = "cloudera";

    @InjectMocks
    private CentosToRedHatUpgradeAvailabilityService underTest;

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stack;

    @Mock
    private Map<String, String> stackRelatedParcels;

    @Mock
    private ImageCatalogPlatform imageCatalogPlatform;

    @BeforeEach
    void before() {
        lenient().when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        lenient().when(stack.getCloudPlatform()).thenReturn(AWS.name());
        lenient().when(stack.getPlatformVariant()).thenReturn(PLATFORM_VARIANT);
    }

    @Test
    void testIsOsUpgradePermittedShouldReturnTrue() {
        Image targetImage = createTargetImage(RHEL8);
        when(lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER)).thenReturn(true);

        assertTrue(underTest.isOsUpgradePermitted(createModelImage(CENTOS7), targetImage, stackRelatedParcels));

        verify(lockedComponentChecker).isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER);
    }

    @Test
    void testIsOsUpgradePermittedShouldReturnFalseWhenTheUpgradePathIsRedhatToCentOs() {
        assertFalse(underTest.isOsUpgradePermitted(createModelImage(RHEL8), createTargetImage(CENTOS7), stackRelatedParcels));
        verifyNoInteractions(lockedComponentChecker);
    }

    @Test
    void testIsOsUpgradePermittedShouldReturnFalseWhenTheComponentVersionsAreNotMatching() {
        Image targetImage = createTargetImage(RHEL8);
        when(lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER)).thenReturn(false);

        assertFalse(underTest.isOsUpgradePermitted(createModelImage(CENTOS7), targetImage, stackRelatedParcels));

        verify(lockedComponentChecker).isUpgradePermitted(targetImage, stackRelatedParcels, CM_BUILD_NUMBER);
    }

    @Test
    void testIsHelperImageAvailableShouldReturnTrue() {
        Image image1 = createImage("image1", CENTOS7);
        Image image2 = createImage("image2", CENTOS7);
        Image image3 = createImage("image3", RHEL8);
        Image targetImage = createTargetImage(RHEL8);

        Map<String, String> packageVersions = Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3");
        when(lockedComponentChecker.isUpgradePermitted(image1, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);
        when(lockedComponentChecker.isUpgradePermitted(image2, packageVersions, CM_BUILD_NUMBER)).thenReturn(true);

        assertTrue(underTest.isHelperImageAvailable(List.of(image1, image2, image3), targetImage, STACK_RELATED_PARCELS));

        verify(lockedComponentChecker, times(2)).isUpgradePermitted(any(), anyMap(), anyString());
    }

    @Test
    void testIsHelperImageAvailableShouldReturnFalseWhenThereIsNoCentOsImageAvailableWithTheSamePackages() {
        Image image1 = createImage("image1", CENTOS7);
        Image image2 = createImage("image2", CENTOS7);
        Image image3 = createImage("image3", RHEL8);
        Image image4 = createImage("image4", RHEL8);
        Image targetImage = createTargetImage(RHEL8);

        Map<String, String> packageVersions = Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3");
        when(lockedComponentChecker.isUpgradePermitted(image1, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);
        when(lockedComponentChecker.isUpgradePermitted(image2, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);

        assertFalse(underTest.isHelperImageAvailable(List.of(image1, image2, image3, image4), targetImage, STACK_RELATED_PARCELS));

        verify(lockedComponentChecker, times(2)).isUpgradePermitted(any(), anyMap(), anyString());
    }

    @Test
    void testIsHelperImageAvailableShouldReturnFalseWhenThereIsNoCentOsImageAvailable() {
        Image image1 = createImage("image1", RHEL8);
        Image image2 = createImage("image2", RHEL8);
        Image image3 = createImage("image3", RHEL8);
        Image targetImage = createTargetImage(RHEL8);

        assertFalse(underTest.isHelperImageAvailable(List.of(image1, image2, image3), targetImage, STACK_RELATED_PARCELS));

        verifyNoInteractions(lockedComponentChecker);
    }

    @Test
    void testFindHelperImageIfNecessaryShouldReturnAnImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image1 = createImage("image1", CENTOS7);
        Image image2 = createImage("image2", CENTOS7);
        Image image3 = createImage("image3", RHEL8);
        List<Image> candidateImages = List.of(image1, image2, image3);

        UpgradeImageInfo upgradeImageInfo = createUpgradeImageInfo(CENTOS7, RHEL8);
        when(upgradeImageInfoFactory.create(TARGET_IMAGE_ID, STACK_ID)).thenReturn(upgradeImageInfo);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(AWS.name(), PLATFORM_VARIANT)).thenReturn(Set.of(imageCatalogPlatform));
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform))).thenReturn(candidateImages);

        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3"));
        Map<String, String> packageVersions = Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3");
        when(lockedComponentChecker.isUpgradePermitted(image1, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);
        when(lockedComponentChecker.isUpgradePermitted(image2, packageVersions, CM_BUILD_NUMBER)).thenReturn(true);

        Optional<Image> actual = doAs(ACTOR, () -> underTest.findHelperImageIfNecessary(TARGET_IMAGE_ID, STACK_ID));

        assertTrue(actual.isPresent());
        assertEquals(image2.getUuid(), actual.get().getUuid());
    }

    @Test
    void testFindHelperImageIfNecessaryShouldReturnOptionalEmptyWhenWhereIsNoImageWithTheSamePackages()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image1 = createImage("image1", CENTOS7);
        Image image2 = createImage("image2", CENTOS7);
        Image image3 = createImage("image3", RHEL8);
        List<Image> candidateImages = List.of(image1, image2, image3);

        UpgradeImageInfo upgradeImageInfo = createUpgradeImageInfo(CENTOS7, RHEL8);
        when(upgradeImageInfoFactory.create(TARGET_IMAGE_ID, STACK_ID)).thenReturn(upgradeImageInfo);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(AWS.name(), PLATFORM_VARIANT)).thenReturn(Set.of(imageCatalogPlatform));
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform))).thenReturn(candidateImages);

        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3"));
        Map<String, String> packageVersions = Map.of(CDH.name(), CDH_VERSION, CFM.getKey(), "1.2.3");
        when(lockedComponentChecker.isUpgradePermitted(image1, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);
        when(lockedComponentChecker.isUpgradePermitted(image2, packageVersions, CM_BUILD_NUMBER)).thenReturn(false);

        assertTrue(doAs(ACTOR, () -> underTest.findHelperImageIfNecessary(TARGET_IMAGE_ID, STACK_ID)).isEmpty());
    }

    private Image createImage(String imageId, OsType os) {
        return Image.builder().withUuid(imageId).withOs(os.getOs()).withOsType(os.getOsType()).build();
    }

    private Image createTargetImage(OsType os) {
        return Image.builder()
                .withUuid(TARGET_IMAGE_ID)
                .withOs(os.getOs())
                .withOsType(os.getOsType())
                .withPackageVersions(Map.of(STACK.getKey(), "7.2.17", CFM.getKey(), "1.2.3"))
                .withCmBuildNumber(CM_BUILD_NUMBER)
                .withStackDetails(new ImageStackDetails(null, new StackRepoDetails(Map.of(REPOSITORY_VERSION, CDH_VERSION), Collections.emptyMap()), null))
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createModelImage(OsType os) {
        return ModelImageTestBuilder.builder()
                .withOs(os.getOs())
                .withOsType(os.getOsType())
                .withPackageVersions(Map.of(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), CM_BUILD_NUMBER))
                .build();
    }

    private UpgradeImageInfo createUpgradeImageInfo(OsType currentOS, OsType targetOs) {
        return new UpgradeImageInfo(createModelImage(currentOS), StatedImage.statedImage(createTargetImage(targetOs), null, IMAGE_CATALOG_NAME));
    }
}