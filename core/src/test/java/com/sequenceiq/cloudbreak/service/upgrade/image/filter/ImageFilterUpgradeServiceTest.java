package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
public class ImageFilterUpgradeServiceTest {

    private static final String REASON_MESSAGE = "reason message";

    private ImageFilterUpgradeService underTest;

    @Mock
    private CloudPlatformBasedUpgradeImageFilter cloudPlatformBasedUpgradeImageFilter;

    @Mock
    private CmAndStackVersionUpgradeImageFilter cmAndStackVersionUpgradeImageFilter;

    @Mock
    private CurrentImageUpgradeImageFilter currentImageUpgradeImageFilter;

    @Mock
    private EntitlementDrivenPackageLocationFilter entitlementDrivenPackageLocationFilter;

    @Mock
    private IgnoredCmVersionUpgradeImageFilter ignoredCmVersionUpgradeImageFilter;

    @Mock
    private ImageCreationBasedUpgradeImageFilter imageCreationBasedUpgradeImageFilter;

    @Mock
    private NonCmUpgradeImageFilter nonCmUpgradeImageFilter;

    @Mock
    private OsVersionBasedUpgradeImageFilter osVersionBasedUpgradeImageFilter;

    @Mock
    private ImageFilterParams imageFilterParams;

    private final List<Image> availableImages = List.of(createImage("image1"), createImage("image2"));

    private final ImageFilterResult imageFilterResultWithCandidates = new ImageFilterResult(availableImages);

    private final ImageFilterResult emptyResult = new ImageFilterResult(Collections.emptyList(), REASON_MESSAGE);

    @BeforeEach
    public void before() {
        underTest = new ImageFilterUpgradeService(List.of(currentImageUpgradeImageFilter, cloudPlatformBasedUpgradeImageFilter, nonCmUpgradeImageFilter,
                ignoredCmVersionUpgradeImageFilter, imageCreationBasedUpgradeImageFilter, cmAndStackVersionUpgradeImageFilter, osVersionBasedUpgradeImageFilter,
                entitlementDrivenPackageLocationFilter));
    }

    @Test
    public void testFilterImagesShouldReturnAllImageWhenAllImageIsEligibleForUpgrade() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(imageCreationBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cmAndStackVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(osVersionBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(entitlementDrivenPackageLocationFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEquals(availableImages, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(imageCreationBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cmAndStackVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(osVersionBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(entitlementDrivenPackageLocationFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenCurrentImageFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(cloudPlatformBasedUpgradeImageFilter);
        verifyNoInteractions(nonCmUpgradeImageFilter);
        verifyNoInteractions(ignoredCmVersionUpgradeImageFilter);
        verifyNoInteractions(imageCreationBasedUpgradeImageFilter);
        verifyNoInteractions(cmAndStackVersionUpgradeImageFilter);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenCloudPlatformFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(nonCmUpgradeImageFilter);
        verifyNoInteractions(ignoredCmVersionUpgradeImageFilter);
        verifyNoInteractions(imageCreationBasedUpgradeImageFilter);
        verifyNoInteractions(cmAndStackVersionUpgradeImageFilter);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenNonCmFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(ignoredCmVersionUpgradeImageFilter);
        verifyNoInteractions(imageCreationBasedUpgradeImageFilter);
        verifyNoInteractions(cmAndStackVersionUpgradeImageFilter);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenIgnoredCmVersionFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(imageCreationBasedUpgradeImageFilter);
        verifyNoInteractions(cmAndStackVersionUpgradeImageFilter);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenImageCreationBasedFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(imageCreationBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(imageCreationBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(cmAndStackVersionUpgradeImageFilter);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenCmAndStackVersionFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(imageCreationBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cmAndStackVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(imageCreationBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cmAndStackVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(osVersionBasedUpgradeImageFilter);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenOsVersionBasedFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(imageCreationBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cmAndStackVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(osVersionBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(imageCreationBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cmAndStackVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(osVersionBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verifyNoInteractions(entitlementDrivenPackageLocationFilter);
    }

    @Test
    public void testFilterImagesShouldReturnErrorMessageWhenPackageLocationFilterDoesNotReturnImages() {
        when(currentImageUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cloudPlatformBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(nonCmUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(ignoredCmVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(imageCreationBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(cmAndStackVersionUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(osVersionBasedUpgradeImageFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(imageFilterResultWithCandidates);
        when(entitlementDrivenPackageLocationFilter.filter(imageFilterResultWithCandidates, imageFilterParams)).thenReturn(emptyResult);

        ImageFilterResult actual = underTest.filterImages(imageFilterResultWithCandidates, imageFilterParams);

        assertEmptyResultWithReason(actual);
        verify(currentImageUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cloudPlatformBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(nonCmUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(ignoredCmVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(imageCreationBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(cmAndStackVersionUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(osVersionBasedUpgradeImageFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
        verify(entitlementDrivenPackageLocationFilter).filter(imageFilterResultWithCandidates, imageFilterParams);
    }

    private void assertEmptyResultWithReason(ImageFilterResult actual) {
        assertEquals(REASON_MESSAGE, actual.getReason());
        assertTrue(actual.getImages().isEmpty());
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, true, null, null);
    }

}