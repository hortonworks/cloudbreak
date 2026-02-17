package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeOsVersionFilterCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class OsVersionBasedUpgradeImageFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final OsType CURRENT_OS = OsType.CENTOS7;

    private static final long STACK_ID = 1L;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Mock
    private ClusterUpgradeOsVersionFilterCondition clusterUpgradeOsVersionFilterCondition;

    @InjectMocks
    private OsVersionBasedUpgradeImageFilter underTest;

    @Test
    public void testFilterShouldReturnAllImages() {
        List<Image> images = List.of(createImage("image1", CURRENT_OS), createImage("image2", CURRENT_OS));
        when(clusterUpgradeOsVersionFilterCondition.isImageAllowed(any(), anyString(), any(), anyBoolean(), anySet())).thenReturn(true);

        ImageFilterResult actual = testFilterImages(createImageFilterParams(), images);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testRHelToRhel8AndRhel9UpgradeIsAllowed() {
        when(entitlementService.isEntitledToUseOS(any(), eq(OsType.RHEL9))).thenReturn(true);
        Set<OsType> usedOsTypes = Set.of(OsType.RHEL8);
        when(currentImageUsageCondition.getOSUsedByInstances(any())).thenReturn(usedOsTypes);
        Image image1 = createImage("image1", OsType.CENTOS7);
        Image image2 = createImage("image2", OsType.RHEL8);
        Image image3 = createImage("image3", OsType.RHEL9);
        List<Image> images = List.of(image1, image2, image3);

        when(clusterUpgradeOsVersionFilterCondition.isImageAllowed(OsType.RHEL8, Architecture.X86_64.getName(), image1, true, usedOsTypes)).thenReturn(false);
        when(clusterUpgradeOsVersionFilterCondition.isImageAllowed(OsType.RHEL8, Architecture.X86_64.getName(), image2, true, usedOsTypes)).thenReturn(true);
        when(clusterUpgradeOsVersionFilterCondition.isImageAllowed(OsType.RHEL8, Architecture.X86_64.getName(), image3, true, usedOsTypes)).thenReturn(true);

        ImageFilterParams imageFilterParams = createImageFilterParams(OsType.RHEL8);
        ImageFilterResult actual = testFilterImages(imageFilterParams, images);

        assertEquals(List.of(image2, image3), actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    private ImageFilterParams createImageFilterParams() {
        return createImageFilterParams(OsType.CENTOS7);
    }

    private ImageFilterParams createImageFilterParams(OsType osType) {
        return new ImageFilterParams(null, createCurrentImage(osType), null, false, false, null, null, null, STACK_ID, null, null, null, null, false);
    }

    private Image createImage(String imageId, OsType osType) {
        return Image.builder().withUuid(imageId).withOs(osType.getOs()).withOsType(osType.getOsType()).withArchitecture(Architecture.X86_64.getName()).build();
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