package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@ExtendWith(MockitoExtension.class)
public class MixedPackageVersionServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CM_VERSION = "7.2.0";

    private static final String CDH_VERSION = "7.2.2";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_CATALOG_URL = "http://imagecatalog";

    private static final String CURRENT_IMAGE_ID = "current-image";

    private static final String TARGET_IMAGE_ID = "target-image";

    private static final String CDH_KEY = "CDH";

    @InjectMocks
    private MixedPackageVersionService underTest;

    @Mock
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Mock
    private TargetImageAwareMixedPackageVersionService targetImageAwareMixedPackageVersionService;

    @Mock
    private CandidateImageAwareMixedPackageVersionService candidateImageAwareMixedPackageVersionService;

    @Test
    void testValidatePackageVersionsShouldNotSendNotificationWhenThePackageVersionsAreValidAndEqualsWithTheCurrentImage()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Map<String, String> parcels = Map.of(CDH_KEY, CDH_VERSION, "SPARK", "3.1.5");
        CmSyncOperationResult cmSyncOperationResult = createCmSyncResult(CM_VERSION, parcels);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentImage = createCatalogImage(CM_VERSION);

        when(imageService.getImage(STACK_ID)).thenReturn(createModelImage(CURRENT_IMAGE_ID));
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID)).thenReturn(createStatedImage(currentImage));
        when(clouderaManagerProductTransformer.transformToMap(currentImage, true, true)).thenReturn(parcels);
        Set<ParcelInfo> activeParcels = cmSyncOperationResult.getCmParcelSyncOperationResult().getActiveParcels();
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels)).thenReturn(true);

        underTest.validatePackageVersions(STACK_ID, cmSyncOperationResult, Collections.emptySet());

        verify(imageService).getImage(STACK_ID);
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID);
        verify(clouderaManagerProductTransformer).transformToMap(currentImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels);
        verifyNoInteractions(clusterUpgradeTargetImageService);
    }

    @Test
    void testValidatePackageVersionsShouldExamineCandidateImagesWhenTheCurrentImageIsNotFound()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Map<String, String> parcels = Map.of(CDH_KEY, CDH_VERSION, "SPARK", "3.1.5");
        CmSyncOperationResult cmSyncOperationResult = createCmSyncResult(CM_VERSION, parcels);
        Set<ParcelInfo> activeParcels = cmSyncOperationResult.getCmParcelSyncOperationResult().getActiveParcels();
        Set<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> candidateImages = Set.of(createCatalogImage("image1", CM_VERSION));

        when(imageService.getImage(STACK_ID)).thenReturn(createModelImage(CURRENT_IMAGE_ID));
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID)).thenReturn(null);
        when(imageCatalogService.getCloudbreakDefaultImageCatalog()).thenReturn(createImageCatalog());

        underTest.validatePackageVersions(STACK_ID, cmSyncOperationResult, candidateImages);

        verify(imageService).getImage(STACK_ID);
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID);
        verify(imageCatalogService).getCloudbreakDefaultImageCatalog();
        verify(candidateImageAwareMixedPackageVersionService).examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, CM_VERSION, activeParcels,
                IMAGE_CATALOG_URL);
        verifyNoInteractions(clusterUpgradeTargetImageService);
    }

    @Test
    void testValidatePackageVersionsShouldExamineTargetImageWhenTheTargetImageIsPresent()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Map<String, String> parcels = Map.of(CDH_KEY, CDH_VERSION, "SPARK", "3.1.5");
        CmSyncOperationResult cmSyncOperationResult = createCmSyncResult(CM_VERSION, parcels);
        Set<ParcelInfo> activeParcels = cmSyncOperationResult.getCmParcelSyncOperationResult().getActiveParcels();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentImage = createCatalogImage(CM_VERSION);

        when(imageService.getImage(STACK_ID)).thenReturn(createModelImage(CURRENT_IMAGE_ID));
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID)).thenReturn(createStatedImage(currentImage));
        when(clouderaManagerProductTransformer.transformToMap(currentImage, true, true)).thenReturn(parcels);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels)).thenReturn(false);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetImage = createCatalogImage(CM_VERSION);
        when(clusterUpgradeTargetImageService.findTargetImage(STACK_ID)).thenReturn(Optional.of(createModelImage(TARGET_IMAGE_ID)));
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(createStatedImage(targetImage));

        underTest.validatePackageVersions(STACK_ID, cmSyncOperationResult, Collections.emptySet());

        verify(imageService).getImage(STACK_ID);
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID);
        verify(clouderaManagerProductTransformer).transformToMap(currentImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels);
        verify(clusterUpgradeTargetImageService).findTargetImage(STACK_ID);
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID);
        verify(targetImageAwareMixedPackageVersionService).examinePackageVersionsWithTargetImage(STACK_ID, targetImage, CM_VERSION, activeParcels);
    }

    @Test
    void testValidatePackageVersionsShouldExamineCandidateImagesWhenTheTargetImageIsNotPresent()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Map<String, String> parcels = Map.of(CDH_KEY, CDH_VERSION, "SPARK", "3.1.5");
        CmSyncOperationResult cmSyncOperationResult = createCmSyncResult(CM_VERSION, parcels);
        Set<ParcelInfo> activeParcels = cmSyncOperationResult.getCmParcelSyncOperationResult().getActiveParcels();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentImage = createCatalogImage(CM_VERSION);
        Set<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> candidateImages = Set.of(createCatalogImage("image1", CM_VERSION));

        when(imageService.getImage(STACK_ID)).thenReturn(createModelImage(CURRENT_IMAGE_ID));
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID)).thenReturn(createStatedImage(currentImage));
        when(clouderaManagerProductTransformer.transformToMap(currentImage, true, true)).thenReturn(parcels);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels)).thenReturn(false);
        when(clusterUpgradeTargetImageService.findTargetImage(STACK_ID)).thenReturn(Optional.empty());

        underTest.validatePackageVersions(STACK_ID, cmSyncOperationResult, candidateImages);

        verify(imageService).getImage(STACK_ID);
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID);
        verify(clouderaManagerProductTransformer).transformToMap(currentImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION, parcels, CM_VERSION, activeParcels);
        verify(clusterUpgradeTargetImageService).findTargetImage(STACK_ID);
        verify(candidateImageAwareMixedPackageVersionService).examinePackageVersionsWithAllCandidateImages(STACK_ID, candidateImages, CM_VERSION, activeParcels,
                IMAGE_CATALOG_URL);
    }

    @Test
    void testValidatePackageVersionsShouldNotCallAnyExaminationWhenTheActiveCmVersionIsNotAvailable() {
        CmSyncOperationResult cmSyncOperationResult = createCmSyncResult(null, Collections.emptyMap());
        Set<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> candidateImages = Set.of(createCatalogImage("image1", CM_VERSION));

        underTest.validatePackageVersions(STACK_ID, cmSyncOperationResult, candidateImages);

        verifyNoInteractions(clusterUpgradeTargetImageService);
        verifyNoInteractions(clouderaManagerProductTransformer);
        verifyNoInteractions(imageService);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(mixedPackageVersionComparator);
        verifyNoInteractions(targetImageAwareMixedPackageVersionService);
        verifyNoInteractions(candidateImageAwareMixedPackageVersionService);
    }

    private StatedImage createStatedImage(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return StatedImage.statedImage(image, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createCatalogImage(String imageId, String cmVersion) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, null, null, imageId, null, null, null, null, null,
                Map.of(CM.getKey(), cmVersion), null, Collections.emptyList(), null, false, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createCatalogImage(String cmVersion) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(null, null, null, null, null, null, null, null, null, null, null,
                Map.of(CM.getKey(), cmVersion), null, Collections.emptyList(), null, false, null, null);
    }

    private Image createModelImage(String imageId) {
        return new Image(null, null, null, null, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, imageId, null);
    }

    private CmSyncOperationResult createCmSyncResult(String cmVersion, Map<String, String> parcelVersionsByName) {
        return new CmSyncOperationResult(new CmRepoSyncOperationResult(cmVersion, null), createCmParcelSyncResult(parcelVersionsByName));
    }

    private CmParcelSyncOperationResult createCmParcelSyncResult(Map<String, String> parcelVersionsByName) {
        return new CmParcelSyncOperationResult(parcelVersionsByName.entrySet().stream()
                .map(parcel -> new ParcelInfo(parcel.getKey(), parcel.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new)),
                Collections.emptySet());
    }

    private ImageCatalog createImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);
        return imageCatalog;
    }
}