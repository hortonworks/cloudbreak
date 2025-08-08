package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeUtil;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class OsChangeUpgradeImageFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String VERSION_7_2_16 = "7.2.16";

    private static final String VERSION_7_2_17 = "7.2.17";

    private static final String VERSION_7_2_18 = "7.2.18";

    private static final long STACK_ID = 1L;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private OsChangeUtil osChangeUtil;

    @InjectMocks
    private OsChangeUpgradeImageFilter underTest;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    private AtomicLong imageCreationTimeSequence = new AtomicLong(1);

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_16_toRedHat_7_2_17_UpgradeIsNotAllowed() {
        //CHECKSTYLE:ON
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_16);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_16_toRedHat_7_2_17_UpgradeIsAllowedWhenCentOSImageIsAvailableWithTheSamePackages() {
        //CHECKSTYLE:ON
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_16);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        Image centosImage = centOSCatalogImage(VERSION_7_2_17);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));
        when(osChangeUtil.isOsUpgradePermitted(STACK_ID, imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(false);
        when(osChangeUtil.isHelperImageAvailable(List.of(redhatImage, centosImage), redhatImage,
                imageFilterParams.getStackRelatedParcels().keySet(), OsType.CENTOS7)).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage, centosImage);

        assertEquals(List.of(redhatImage, centosImage), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_16_toRedHat_7_2_17_UpgradeIsAllowedWhenCentOSImageIsAvailableWithTheSamePackagesAndTargetImageIsPresent() {
        //CHECKSTYLE:ON
        //ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_16);
        ImageFilterParams imageFilterParams = new ImageFilterParams("target-image-id", image(OsType.CENTOS7.getOs(), OsType.CENTOS7.getOsType(), VERSION_7_2_16),
                "image-catalog-name", false, false, Map.of(CDH.name(), "7.2.17"), null, null, STACK_ID, null, null, null, null, false);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        Image centosImage = centOSCatalogImage(VERSION_7_2_17);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));
        when(osChangeUtil.isOsUpgradePermitted(STACK_ID, imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(false);
        when(osChangeUtil.isHelperImageAvailable(STACK_ID, "image-catalog-name", redhatImage,
                imageFilterParams.getStackRelatedParcels().keySet(), OsType.CENTOS7)).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage, centosImage);

        assertEquals(List.of(redhatImage, centosImage), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_17_toRedHat_7_2_17_UpgradeIsAllowed() {
        //CHECKSTYLE:ON
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_17);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));
        when(osChangeUtil.isOsUpgradePermitted(STACK_ID, imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(redhatImage), result.getImages());
    }

    @Test
    public void testRedHat8ImageIsAllowedWhenCurrentImageIsRedHat() {
        ImageFilterParams imageFilterParams = redhatImageFilterParams(VERSION_7_2_17);
        Image redhatImage1 = redhatCatalogImage(VERSION_7_2_17);
        Image redhatImage2 = redhatCatalogImage(VERSION_7_2_18);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.RHEL8));

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage1, redhatImage2);

        assertEquals(List.of(redhatImage1, redhatImage2), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_17_toRedHat_7_2_17_UpgradeIsAllowedAndForced() {
        //CHECKSTYLE:ON
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_17);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        Image centOSImage1 = centOSCatalogImage(VERSION_7_2_17);
        Image centOSImage2 = centOSCatalogImage(VERSION_7_2_18);
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));
        when(osChangeUtil.isOsUpgradePermitted(STACK_ID, imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, centOSImage1, redhatImage, centOSImage2);

        assertEquals(List.of(centOSImage1, redhatImage, centOSImage2), result.getImages());
    }

    private ImageFilterResult testImageFiltering(ImageFilterParams imageFilterParams, Image... images) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(new ImageFilterResult(Arrays.asList(images)), imageFilterParams));
    }

    private ImageFilterParams centosImageFilterParams(String version) {
        return imageFilterParams(image(OsType.CENTOS7.getOs(), OsType.CENTOS7.getOsType(), version));
    }

    private ImageFilterParams redhatImageFilterParams(String version) {
        return imageFilterParams(image(OsType.RHEL8.getOs(), OsType.RHEL8.getOsType(), version));
    }

    private ImageFilterParams imageFilterParams(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return new ImageFilterParams(null, image, null, false, false, Map.of(CDH.name(), "7.2.17"), null, null, STACK_ID, null, null, null, null, false);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image image(String os, String osType, String version) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withOs(os)
                .withOsType(osType)
                .withPackageVersions(Map.of("stack", version))
                .build();
    }

    private Image redhatCatalogImage(String version) {
        return catalogImage(OsType.RHEL8.getOs(), OsType.RHEL8.getOsType(), version);
    }

    private Image centOSCatalogImage(String version) {
        return catalogImage(OsType.CENTOS7.getOs(), OsType.CENTOS7.getOsType(), version);
    }

    private Image catalogImage(String os, String osType, String version) {
        return Image.builder()
                .withCreated(imageCreationTimeSequence.getAndIncrement())
                .withOs(os)
                .withOsType(osType)
                .withVersion(version)
                .build();
    }
}