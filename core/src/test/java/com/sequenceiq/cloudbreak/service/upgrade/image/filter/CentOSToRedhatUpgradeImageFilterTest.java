package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentOSToRedHatUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class CentOSToRedHatUpgradeImageFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String VERSION_7_2_16 = "7.2.16";

    private static final String VERSION_7_2_17 = "7.2.17";

    private static final String VERSION_7_2_18 = "7.2.18";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CentOSToRedHatUpgradeAvailabilityService centOSToRedHatUpgradeAvailabilityService;

    @InjectMocks
    private CentOSToRedHatUpgradeImageFilter underTest;

    private AtomicLong imageCreationTimeSequence = new AtomicLong(1);

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_16_toRedHat_7_2_17_UpgradeIsNotAllowed() {
        //CHECKSTYLE:ON
        redhatImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_16);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_16_toRedHat_7_2_17_UpgradeIsAllowedWhenCentOSImageIsAvailableWithTheSamePackages() {
        //CHECKSTYLE:ON
        redhatImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_16);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        Image centosImage = centOSCatalogImage(VERSION_7_2_17);
        when(centOSToRedHatUpgradeAvailabilityService.isOsUpgradePermitted(imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(false);
        when(centOSToRedHatUpgradeAvailabilityService.isHelperImageAvailable(List.of(redhatImage, centosImage), redhatImage,
                imageFilterParams.getStackRelatedParcels().keySet())).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage, centosImage);

        assertEquals(List.of(redhatImage, centosImage), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_17_toRedHat_7_2_17_UpgradeIsAllowed() {
        //CHECKSTYLE:ON
        redhatImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_17);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        when(centOSToRedHatUpgradeAvailabilityService.isOsUpgradePermitted(imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(redhatImage), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_17_toRedHat_7_2_17_UpgradeIsNotAllowedWhenCentOSIsPreferred() {
        //CHECKSTYLE:ON
        centOSImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_17);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_18_toRedHat_7_2_18_UpgradeIsNotAllowedWhenCentOSIsPreferred() {
        //CHECKSTYLE:ON
        centOSImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_18);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_18);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage);

        assertEquals(List.of(), result.getImages());
    }

    @Test
    public void testRedHatImageIsAllowedWhenCurrentImageIsRedHat() {
        ImageFilterParams imageFilterParams = redhatImageFilterParams(VERSION_7_2_17);
        Image redhatImage1 = redhatCatalogImage(VERSION_7_2_17);
        Image redhatImage2 = redhatCatalogImage(VERSION_7_2_18);

        ImageFilterResult result = testImageFiltering(imageFilterParams, redhatImage1, redhatImage2);

        assertEquals(List.of(redhatImage1, redhatImage2), result.getImages());
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCentOS_7_2_17_toRedHat_7_2_17_UpgradeIsAllowedAndForced() {
        //CHECKSTYLE:ON
        redhatImageIsPreferred();
        ImageFilterParams imageFilterParams = centosImageFilterParams(VERSION_7_2_17);
        Image redhatImage = redhatCatalogImage(VERSION_7_2_17);
        Image centOSImage1 = centOSCatalogImage(VERSION_7_2_17);
        Image centOSImage2 = centOSCatalogImage(VERSION_7_2_18);
        when(centOSToRedHatUpgradeAvailabilityService.isOsUpgradePermitted(imageFilterParams.getCurrentImage(), redhatImage,
                imageFilterParams.getStackRelatedParcels())).thenReturn(true);

        ImageFilterResult result = testImageFiltering(imageFilterParams, centOSImage1, redhatImage, centOSImage2);

        assertEquals(List.of(centOSImage1, redhatImage, centOSImage2), result.getImages());
    }

    private ImageFilterResult testImageFiltering(ImageFilterParams imageFilterParams, Image... images) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(new ImageFilterResult(Arrays.asList(images)), imageFilterParams));
    }

    private ImageFilterParams centosImageFilterParams(String version) {
        return imageFilterParams(image(CentOSToRedHatUpgradeImageFilter.CENTOS_7, CentOSToRedHatUpgradeImageFilter.REDHAT_7, version));
    }

    private ImageFilterParams redhatImageFilterParams(String version) {
        return imageFilterParams(image(CentOSToRedHatUpgradeImageFilter.REDHAT_8, CentOSToRedHatUpgradeImageFilter.REDHAT_8, version));
    }

    private ImageFilterParams imageFilterParams(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return new ImageFilterParams(null, image, null, false, Map.of(CDH.name(), "7.2.17"), null, null, null, null, null, null, null, false);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image image(String os, String osType, String version) {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, os, osType, null, null, null, Map.of("stack", version), null, null);
    }

    private Image redhatCatalogImage(String version) {
        return catalogImage(CentOSToRedHatUpgradeImageFilter.REDHAT_8, CentOSToRedHatUpgradeImageFilter.REDHAT_8, version);
    }

    private Image centOSCatalogImage(String version) {
        return catalogImage(CentOSToRedHatUpgradeImageFilter.CENTOS_7, CentOSToRedHatUpgradeImageFilter.REDHAT_7, version);
    }

    private Image catalogImage(String os, String osType, String version) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(
                null, imageCreationTimeSequence.getAndIncrement(), null, null, os, null, version, null, null, null, osType, null, null, null, null, false, null,
                null);
    }

    private void redhatImageIsPreferred() {
        when(entitlementService.isRhel8ImagePreferred(anyString())).thenReturn(true);
    }

    private void centOSImageIsPreferred() {
        when(entitlementService.isRhel8ImagePreferred(anyString())).thenReturn(false);
    }
}