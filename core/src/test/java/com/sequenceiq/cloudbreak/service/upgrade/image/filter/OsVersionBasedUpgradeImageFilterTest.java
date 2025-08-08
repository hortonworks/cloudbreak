package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUpgradeCondition;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class OsVersionBasedUpgradeImageFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final OsType CURRENT_OS = OsType.CENTOS7;

    private static final long STACK_ID = 1L;

    @Mock
    private OsChangeUpgradeCondition osChangeUpgradeCondition;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @InjectMocks
    private OsVersionBasedUpgradeImageFilter underTest;

    @Test
    public void testFilterShouldReturnAllImages() {
        List<Image> images = List.of(createImage("image1", CURRENT_OS), createImage("image2", CURRENT_OS));

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), images);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsType() {
        Image image1 = createImage("image1", CURRENT_OS);
        Image image2 = createImage("image2", "ubuntu", "amazon-linux");

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), List.of(image1, image2));

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsTypeWheOnlyTheOsIsDifferent() {
        Image image1 = createImage("image1", CURRENT_OS);
        Image image2 = createImage("image2", "ubuntu", CURRENT_OS.getOsType());

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), List.of(image1, image2));

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsTypeWheOnlyTheOsTypeIsDifferent() {
        Image image1 = createImage("image1", CURRENT_OS);
        Image image2 = createImage("image2", CURRENT_OS.getOs(), "amazon-linux");

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), List.of(image1, image2));

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenThereAreNoImageWithSameOsAndOsType() {
        List<Image> images = List.of(createImage("image1", CURRENT_OS.getOs(), "ubuntu"), createImage("image2", CURRENT_OS.getOs(), "amazon-linux"));

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), images);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
    }

    @Test
    public void testCentOsToRhel9UpgradeIsNotAllowed() {
        List<Image> images = List.of(createImage("image1", OsType.RHEL9));

        ImageFilterParams imageFilterParams = createImageFilterParams(OsType.CENTOS7);
        ImageFilterResult actual = testFilterImages(imageFilterParams, images);

        assertEquals(List.of(), actual.getImages());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
    }

    @Test
    public void testCentOsToRhel9UpgradeIsNotAllowedWhenEntitlementIsEnabled() {
        when(entitlementService.isEntitledToUseOS(any(), eq(OsType.RHEL9))).thenReturn(true);
        List<Image> images = List.of(createImage("image1", OsType.RHEL9));

        ImageFilterParams imageFilterParams = createImageFilterParams(OsType.CENTOS7);
        ImageFilterResult actual = testFilterImages(imageFilterParams, images);

        assertEquals(List.of(), actual.getImages());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
    }

    @Test
    public void testRHelToRhel8AndRhel9UpgradeIsAllowed() {
        when(entitlementService.isEntitledToUseOS(any(), eq(OsType.RHEL9))).thenReturn(true);
        Set<OsType> usedOsTypes = Set.of(OsType.RHEL8);
        when(currentImageUsageCondition.getOSUsedByInstances(any())).thenReturn(usedOsTypes);
        Image image1 = createImage("image1", OsType.CENTOS7);
        Image image2 = createImage("image2", OsType.RHEL8);
        Image image3 = createImage("image3", OsType.RHEL9);
        when(osChangeUpgradeCondition.isNextMajorOsImage(usedOsTypes, image1)).thenReturn(false);
        when(osChangeUpgradeCondition.isNextMajorOsImage(usedOsTypes, image3)).thenReturn(true);
        List<Image> images = List.of(image1, image2, image3);

        ImageFilterParams imageFilterParams = createImageFilterParams(OsType.RHEL8);
        ImageFilterResult actual = testFilterImages(imageFilterParams, images);

        assertEquals(List.of(image2, image3), actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testRHelToRhel8AndRhel9UpgradeIsNotAllowedWithoutEntitlement() {
        when(entitlementService.isEntitledToUseOS(any(), eq(OsType.RHEL9))).thenReturn(false);
        Set<OsType> usedOsTypes = Set.of(OsType.RHEL8);
        when(currentImageUsageCondition.getOSUsedByInstances(any())).thenReturn(usedOsTypes);
        Image image1 = createImage("image1", OsType.CENTOS7);
        Image image2 = createImage("image2", OsType.RHEL8);
        Image image3 = createImage("image3", OsType.RHEL9);
        when(osChangeUpgradeCondition.isNextMajorOsImage(usedOsTypes, image1)).thenReturn(false);
        List<Image> images = List.of(image1, image2, image3);

        ImageFilterParams imageFilterParams = createImageFilterParams(OsType.RHEL8);
        ImageFilterResult actual = testFilterImages(imageFilterParams, images);

        assertEquals(List.of(image2), actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    private ImageFilterParams createImageFilterParams() {
        return createImageFilterParams(OsType.CENTOS7);
    }

    private ImageFilterParams createImageFilterParams(OsType osType) {
        return new ImageFilterParams(null, createCurrentImage(osType), null, false, false, null, null, null, STACK_ID, null, null, null, null, false);
    }

    private Image createImage(String imageId, OsType osType) {
        return Image.builder().withUuid(imageId).withOs(osType.getOs()).withOsType(osType.getOsType()).build();
    }

    private Image createImage(String imageId, String os, String osType) {
        return Image.builder().withUuid(imageId).withOs(os).withOsType(osType).build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return createCurrentImage(CURRENT_OS);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage(OsType osType) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId("current-image").withOs(osType.getOs()).withOsType(osType.getOsType()).build();
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }

    private ImageFilterResult testFilterImages(ImageFilterParams imageFilterParams, List<Image> images) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(createImageFilterResult(images), imageFilterParams));
    }
}