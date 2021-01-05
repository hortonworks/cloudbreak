package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@ExtendWith(MockitoExtension.class)
class EntitlementDrivenPackageLocationFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:321";

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private final EntitlementService entitlementService = mock(EntitlementService.class);

    @BeforeEach
    public void init() {
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(anyString())).thenReturn(Boolean.FALSE);
    }

    @AfterEach
    public void postCheck() {
        verify(entitlementService).isInternalRepositoryForUpgradeAllowed("123");
    }

    @Test
    public void testEnitlementEnabled() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(anyString())).thenReturn(Boolean.TRUE);
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, Set.of(filter));
        Predicate<Image> imagePredicate = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImage(mock(Image.class), DATALAKE_STACK_TYPE));

        boolean result = imagePredicate.test(mock(Image.class));

        assertTrue(result);
        verify(filter, never()).filterImage(any(Image.class), any(Image.class), eq(DATALAKE_STACK_TYPE));
    }

    @Test
    public void testAcceptedImageMultipleFilters() {
        Set<PackageLocationFilter> filters = Set.of(createAcceptingFilter(), createAcceptingFilter(), createAcceptingFilter(), createAcceptingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        Predicate<Image> imagePredicate = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImage(mock(Image.class), DATALAKE_STACK_TYPE));

        boolean result = imagePredicate.test(mock(Image.class));

        assertTrue(result);
        filters.forEach(filter -> verify(filter).filterImage(any(Image.class), any(Image.class), eq(DATALAKE_STACK_TYPE)));
    }

    @Test
    public void testRejectedImageMultipleFilters() {
        Set<PackageLocationFilter> filters = Set.of(createAcceptingFilter(), createAcceptingFilter(), createRejectingFilter(), createAcceptingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        Predicate<Image> imagePredicate = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImage(mock(Image.class), DATALAKE_STACK_TYPE));

        boolean result = imagePredicate.test(mock(Image.class));

        assertFalse(result);
    }

    @Test
    public void testRejectedImageMultipleRejectingFilters() {
        Set<PackageLocationFilter> filters = Set.of(createRejectingFilter(), createRejectingFilter(), createRejectingFilter());
        EntitlementDrivenPackageLocationFilter underTest = new EntitlementDrivenPackageLocationFilter(entitlementService, filters);
        Predicate<Image> imagePredicate = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.filterImage(mock(Image.class), DATALAKE_STACK_TYPE));

        boolean result = imagePredicate.test(mock(Image.class));

        assertFalse(result);
    }

    private PackageLocationFilter createAcceptingFilter() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        lenient().when(filter.filterImage(any(Image.class), any(Image.class), eq(DATALAKE_STACK_TYPE))).thenReturn(Boolean.TRUE);
        return filter;
    }

    private PackageLocationFilter createRejectingFilter() {
        PackageLocationFilter filter = mock(PackageLocationFilter.class);
        lenient().when(filter.filterImage(any(Image.class), any(Image.class), eq(DATALAKE_STACK_TYPE))).thenReturn(Boolean.FALSE);
        return filter;
    }
}