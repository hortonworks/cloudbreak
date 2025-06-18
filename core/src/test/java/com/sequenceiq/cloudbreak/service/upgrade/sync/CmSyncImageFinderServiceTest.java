package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class CmSyncImageFinderServiceTest {

    private static final String REPOSITORY_VERSION = "repository-version";

    private static final String CM_VERSION = "7.2.1";

    private static final String CURRENT_IMAGE_ID = "current-image-id";

    private static final String CM_BUILD_NUMBER = "123456";

    private static final String CDH_VERSION = "7.2.12";

    @InjectMocks
    private CmSyncImageFinderService underTest;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Test
    void testFindTargetImageForImageSync() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertTrue(actual.isPresent());
        assertEquals("image-2", actual.get().getUuid());
    }

    @Test
    void testFindTargetImageForImageSyncWhenArmImageAlsoExists() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"));
        Image image2Arm = createImage("image-2-arm", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"), Architecture.ARM64);
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = new LinkedHashSet<>(List.of(image1, image2Arm, image2, image3));

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertTrue(actual.isPresent());
        assertEquals("image-2", actual.get().getUuid());
    }

    @Test
    void testFindTargetImageForImageSyncWhenDifferentArchitectureExists() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2Arm = createImage("image-2-arm", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"), Architecture.ARM64);
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = new LinkedHashSet<>(List.of(image1, image2Arm, image3));

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertFalse(actual.isPresent());
    }

    @Test
    void testFindTargetImageForImageSyncWhenImageIsArm() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"), Architecture.ARM64);
        Image image2X86 = createImage("image-2", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"));
        Image image2 = createImage("image-2-arm", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"), Architecture.ARM64);
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap(), Architecture.ARM64);
        Set<Image> candidateImages = new LinkedHashSet<>(List.of(image1, image2X86, image2, image3));

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertTrue(actual.isPresent());
        assertEquals("image-2-arm", actual.get().getUuid());
    }

    @Test
    void testFindTargetImageForImageSyncShouldReturnTheLatest() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"));
        Image image3 = createImage("image-3", CM_VERSION, CDH_VERSION, 2L, Map.of("FLINK", "1.2.3"));
        Image image4 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = Set.of(image1, image2, image3, image4);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertTrue(actual.isPresent());
        assertEquals(image3.getUuid(), actual.get().getUuid());
    }

    @Test
    void testFindTargetImageForImageSyncShouldNotReturnImageWhenCmVersionNotMatches() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", "7.5.1", "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", "7.5.1", CDH_VERSION, 1L, Map.of("FLINK", "1.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, "7.5.1", "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertFalse(actual.isPresent());
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

    @Test
    void testFindTargetImageForImageSyncShouldNotReturnImageWhenCdhVersionNotMatches() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", CM_VERSION, "7.2.2", 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, "7.2.14", 1L, Map.of("FLINK", "1.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, "7.2.4", 1L, Collections.emptyMap());
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertFalse(actual.isPresent());
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

    @Test
    void testFindTargetImageForImageSyncShouldNotReturnImageWhenRequiredPreWarmParcelVersionNotMatches() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 1L, Map.of("SPARK", "2.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.2"));
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, false);

        assertFalse(actual.isPresent());
        verify(clouderaManagerProductTransformer, times(6)).transform(any(Image.class), eq(false), eq(true));
    }

    @Test
    void testFindTargetImageForImageSyncShouldReturnCurrentImageImageWhenTheClusterIsDatalake() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 2L, Map.of("SPARK", "2.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.2"));
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, true);

        assertTrue(actual.isPresent());
        assertEquals(CURRENT_IMAGE_ID, actual.get().getUuid());
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

    @Test
    void testFindTargetImageForImageSyncShouldReturnLatestImageWhenTheClusterIsDatalake() {
        Set<ParcelInfo> activeParcels = createActiveParcels(Map.of("CDH", CDH_VERSION, "FLINK", "1.2.3"));
        CmSyncOperationSummary cmSyncResult = createCmSyncResult(CM_VERSION, activeParcels);

        Image image1 = createImage("image-1", CM_VERSION, CDH_VERSION, 1L, Map.of("FLINK", "1.2.4"));
        Image image2 = createImage("image-2", CM_VERSION, CDH_VERSION, 2L, Map.of("SPARK", "2.2.3"));
        Image image3 = createImage(CURRENT_IMAGE_ID, "7.5.1", CDH_VERSION, 1L, Map.of("FLINK", "1.2.2"));
        Set<Image> candidateImages = Set.of(image1, image2, image3);

        Optional<Image> actual = underTest.findTargetImageForImageSync(cmSyncResult, candidateImages, CURRENT_IMAGE_ID, true);

        assertTrue(actual.isPresent());
        assertEquals(image2.getUuid(), actual.get().getUuid());
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

    private Set<ParcelInfo> createActiveParcels(Map<String, String> parcelsWithVersion) {
        return parcelsWithVersion.entrySet().stream()
                .map(entry -> new ParcelInfo(entry.getKey(), entry.getValue(), ParcelStatus.ACTIVATED))
                .collect(Collectors.toSet());
    }

    private Image createImage(String imageId, String cmVersion, String cdhVersion, Long created, Map<String, String> preWarmParcels) {
        Image image = Image.builder()
                .withUuid(imageId)
                .withCreated(created)
                .withPackageVersions(createPackageVersions(cmVersion))
                .withStackDetails(new ImageStackDetails(null, new StackRepoDetails(Map.of(REPOSITORY_VERSION, cdhVersion), null), null))
                .build();
        lenient().when(clouderaManagerProductTransformer.transform(image, false, true)).thenReturn(createCmProducts(preWarmParcels));
        return image;
    }

    private Image createImage(String imageId, String cmVersion, String cdhVersion, Long created, Map<String, String> preWarmParcels, Architecture architecture) {
        Image image = Image.builder()
                .withUuid(imageId)
                .withCreated(created)
                .withArchitecture(architecture.getName())
                .withPackageVersions(createPackageVersions(cmVersion))
                .withStackDetails(new ImageStackDetails(null, new StackRepoDetails(Map.of(REPOSITORY_VERSION, cdhVersion), null), null))
                .build();
        lenient().when(clouderaManagerProductTransformer.transform(image, false, true)).thenReturn(createCmProducts(preWarmParcels));
        return image;
    }

    private Set<ClouderaManagerProduct> createCmProducts(Map<String, String> parcels) {
        return parcels.entrySet().stream()
                .map(entry -> new ClouderaManagerProduct().withName(entry.getKey()).withVersion(entry.getValue()))
                .collect(Collectors.toSet());
    }

    private Map<String, String> createPackageVersions(String cmVersion) {
        return Map.of(ImagePackageVersion.CM.getKey(), cmVersion, ImagePackageVersion.CM_BUILD_NUMBER.getKey(), CM_BUILD_NUMBER);
    }

    private CmSyncOperationSummary createCmSyncResult(String cmVersion, Set<ParcelInfo> activeParcels) {
        return new CmSyncOperationSummary(null, new CmSyncOperationResult(new CmRepoSyncOperationResult(cmVersion + "-" + CM_BUILD_NUMBER, null),
                new CmParcelSyncOperationResult(activeParcels, null)));
    }

}