package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.PackageLocationFilter;

@ExtendWith(MockitoExtension.class)
class EntitlementDrivenPackageLocationFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:321";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageFilterParams imageFilterParams;

    @Mock
    private Image currentImage;

    @Mock
    private Image image1;

    @Mock
    private Image image2;

    @BeforeEach
    public void init() {
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(anyString())).thenReturn(Boolean.FALSE);
        when(imageFilterParams.getCurrentImage()).thenReturn(currentImage);
    }

    @AfterEach
    public void postCheck() {
        verify(entitlementService).isInternalRepositoryForUpgradeAllowed("123");
    }

    @Test
    public void testEntitlementEnabled() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(anyString())).thenReturn(Boolean.TRUE);
        lenient().when(imageFilterParams.getCurrentImage()).thenReturn(currentImage);
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, Set.of(filter));
        List<Image> images = List.of(this.image1, image2);
        ImageFilterResult imageFilterResult = new ImageFilterResult(images);
        ImageFilterResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(imageFilterResult, imageFilterParams));

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
        verify(filter, never()).filterImage(any(Image.class), any(Image.class), any(ImageFilterParams.class));
    }

    @Test
    public void testAcceptedImageMultipleFilters() {
        Set<PackageLocationFilter> filters = Set.of(createAcceptingFilter(), createAcceptingFilter(), createAcceptingFilter(), createAcceptingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        List<Image> images = List.of(this.image1, image2);
        ImageFilterResult imageFilterResult = new ImageFilterResult(images);

        ImageFilterResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(imageFilterResult, imageFilterParams));

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
        filters.forEach(filter -> verify(filter, times(2)).filterImage(any(Image.class), any(Image.class), any(ImageFilterParams.class)));
    }

    @Test
    public void testRejectedImageMultipleFilters() {
        Set<PackageLocationFilter> filters = Set.of(createAcceptingFilter(), createAcceptingFilter(), createRejectingFilter(), createAcceptingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        List<Image> images = List.of(this.image1, image2);
        ImageFilterResult imageFilterResult = new ImageFilterResult(images);

        ImageFilterResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(imageFilterResult, imageFilterParams));

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade because the location of the packages is not appropriate.", actual.getReason());
    }

    @Test
    public void testRejectedImageMultipleRejectingFilters() {
        Set<PackageLocationFilter> filters = Set.of(createRejectingFilter(), createRejectingFilter(), createRejectingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        List<Image> images = List.of(this.image1, image2);
        ImageFilterResult imageFilterResult = new ImageFilterResult(images);

        ImageFilterResult actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filter(imageFilterResult, imageFilterParams));

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade because the location of the packages is not appropriate.", actual.getReason());
    }

    private PackageLocationFilter createAcceptingFilter() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        lenient().when(filter.filterImage(any(Image.class), any(Image.class), any(ImageFilterParams.class))).thenReturn(Boolean.TRUE);
        return filter;
    }

    private PackageLocationFilter createRejectingFilter() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        lenient().when(filter.filterImage(any(Image.class), any(Image.class), any(ImageFilterParams.class))).thenReturn(Boolean.FALSE);
        return filter;
    }
}