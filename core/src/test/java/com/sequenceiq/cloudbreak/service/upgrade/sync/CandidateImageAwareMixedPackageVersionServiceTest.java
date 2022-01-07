package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_MULTIPLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_NO_CANDIDATE;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
public class CandidateImageAwareMixedPackageVersionServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CM_VERSION = "7.2.0";

    private static final String CDH_KEY = "CDH";

    private static final String V_7_2_2 = "7.2.2";

    private static final String IMAGE_CATALOG_URL = "http://imagecatalog";

    @InjectMocks
    private CandidateImageAwareMixedPackageVersionService underTest;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private MixedPackageMessageProvider mixedPackageMessageProvider;

    @Mock
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Test
    void testExaminePackageVersionsWithAllCandidateImagesShouldNotSendNotificationWhenTheVersionsAreValid() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage1 = createCatalogImage("image1", 123L, CM_VERSION);
        Map<String, String> products1 = Map.of(CDH_KEY, "7.2.0");
        when(clouderaManagerProductTransformer.transformToMap(otherImage1, true, true)).thenReturn(products1);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, products1)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage2 = createCatalogImage("image2", 123L, "7.2.7");

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage3 = createCatalogImage("image3", 123L, CM_VERSION);
        Map<String, String> products3 = Map.of(CDH_KEY, "7.2.7");
        when(clouderaManagerProductTransformer.transformToMap(otherImage3, true, true)).thenReturn(products3);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, products3)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image properImage = createCatalogImage("properImage", 123L, CM_VERSION);
        when(clouderaManagerProductTransformer.transformToMap(properImage, true, true)).thenReturn(activeProducts);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, activeProducts)).thenReturn(true);

        Set<Image> candidateImages = new LinkedHashSet<>();
        candidateImages.add(otherImage1);
        candidateImages.add(otherImage2);
        candidateImages.add(otherImage3);
        candidateImages.add(properImage);

        underTest.examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, CM_VERSION, activeParcels, IMAGE_CATALOG_URL);

        verify(clouderaManagerProductTransformer).transformToMap(otherImage1, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, products1);
        verify(clouderaManagerProductTransformer).transformToMap(otherImage3, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, products3);
        verify(clouderaManagerProductTransformer).transformToMap(properImage, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, activeProducts);
        verifyNoInteractions(mixedPackageMessageProvider);
        verifyNoInteractions(eventService);
    }

    @Test
    void testExaminePackageVersionsWithAllCandidateImagesShouldSendNotificationWhenTheVersionsAreNotValidAndThereAreMultipleCandidate() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        String activeCmVersion = "7.4.0";

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage1 = createCatalogImage("image1", 125L, activeCmVersion);
        Map<String, String> products1 = Map.of(CDH_KEY, "7.2.7");
        when(clouderaManagerProductTransformer.transformToMap(otherImage1, true, true)).thenReturn(products1);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, products1)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage2 = createCatalogImage("image2", 123L, "7.2.7");

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage3 = createCatalogImage("image3", 123L, activeCmVersion);
        Map<String, String> products3 = Map.of(CDH_KEY, "7.2.8");
        when(clouderaManagerProductTransformer.transformToMap(otherImage3, true, true)).thenReturn(products3);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, products3)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage4 = createCatalogImage("image4", 124L, activeCmVersion);
        when(clouderaManagerProductTransformer.transformToMap(otherImage4, true, true)).thenReturn(activeProducts);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, activeProducts)).thenReturn(false);

        Set<Image> candidateImages = new LinkedHashSet<>();
        candidateImages.add(otherImage1);
        candidateImages.add(otherImage2);
        candidateImages.add(otherImage3);
        candidateImages.add(otherImage4);

        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.2");
        when(mixedPackageMessageProvider.createSuggestedVersionsMessage(products1, activeParcels, activeCmVersion))
                .thenReturn("Cloudera Manager 7.4.0, CDH 7.2.7");

        underTest.examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, activeCmVersion, activeParcels, IMAGE_CATALOG_URL);

        verify(clouderaManagerProductTransformer, times(2)).transformToMap(otherImage1, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, products1);
        verify(clouderaManagerProductTransformer).transformToMap(otherImage3, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, products3);
        verify(clouderaManagerProductTransformer).transformToMap(otherImage4, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, activeProducts);
        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_MULTIPLE,
                List.of(activeCmVersion, "CDH 7.2.2", "Cloudera Manager 7.4.0, CDH 7.2.7", "http://imagecatalog"));
    }

    @Test
    void testExaminePackageVersionsWithAllCandidateImagesShouldSendNotificationWhenTheVersionsAreNotValidAndThereIsOnlyOneCandidate() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        String activeCmVersion = "7.4.0";

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage1 = createCatalogImage("image1", 125L, V_7_2_2);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage2 = createCatalogImage("image2", 123L, "7.2.7");

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage3 = createCatalogImage("image3", 123L, activeCmVersion);
        Map<String, String> products3 = Map.of(CDH_KEY, "7.2.8");
        when(clouderaManagerProductTransformer.transformToMap(otherImage3, true, true)).thenReturn(products3);
        when(mixedPackageVersionComparator.matchParcelVersions(activeParcels, products3)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage4 = createCatalogImage("image4", 124L, V_7_2_2);

        Set<Image> candidateImages = new LinkedHashSet<>();
        candidateImages.add(otherImage1);
        candidateImages.add(otherImage2);
        candidateImages.add(otherImage3);
        candidateImages.add(otherImage4);

        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.2");
        when(mixedPackageMessageProvider.createSuggestedVersionsMessage(products3, activeParcels, activeCmVersion))
                .thenReturn("Cloudera Manager 7.4.0, CDH 7.2.8");

        underTest.examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, activeCmVersion, activeParcels, IMAGE_CATALOG_URL);

        verify(clouderaManagerProductTransformer, times(2)).transformToMap(otherImage3, true, true);
        verify(mixedPackageVersionComparator).matchParcelVersions(activeParcels, products3);
        verify(mixedPackageMessageProvider).createActiveParcelsMessage(activeParcels);
        verify(mixedPackageMessageProvider).createSuggestedVersionsMessage(products3, activeParcels, activeCmVersion);
        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED,
                List.of("7.4.0", "CDH 7.2.2", "Cloudera Manager 7.4.0, CDH 7.2.8"));
    }

    @Test
    void testExaminePackageVersionsWithAllCandidateImagesShouldSendNotificationWhenTheVersionsAreNotValidAndThereAreNoCandidate() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        String activeCmVersion = "7.4.0";

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage1 = createCatalogImage("image1", 125L, V_7_2_2);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage2 = createCatalogImage("image2", 123L, "7.2.7");
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage3 = createCatalogImage("image3", 123L, V_7_2_2);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage4 = createCatalogImage("image4", 124L, V_7_2_2);

        Set<Image> candidateImages = new LinkedHashSet<>();
        candidateImages.add(otherImage1);
        candidateImages.add(otherImage2);
        candidateImages.add(otherImage3);
        candidateImages.add(otherImage4);

        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.2");

        underTest.examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, activeCmVersion, activeParcels, IMAGE_CATALOG_URL);

        verify(mixedPackageMessageProvider).createActiveParcelsMessage(activeParcels);
        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_NO_CANDIDATE,
                List.of("7.4.0", "CDH 7.2.2"));
    }

    private Set<ParcelInfo> createParcelInfo(Map<String, String> parcels) {
        return parcels.entrySet().stream().map(entry -> new ParcelInfo(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createCatalogImage(String imageId, Long created, String cmVersion) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, created, null, null, null, imageId, null, null, null, null, null,
                Map.of(CM.getKey(), cmVersion), null, Collections.emptyList(), null, false, null, null);
    }

}