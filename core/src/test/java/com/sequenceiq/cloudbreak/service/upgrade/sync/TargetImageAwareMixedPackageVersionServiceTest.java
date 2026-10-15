package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class TargetImageAwareMixedPackageVersionServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CM_VERSION = "7.2.0";

    private static final String CM_BUILD_NUMBER = "123456";

    private static final String CM_VERSION_AND_BUILD = String.format("%s-%s", CM_VERSION, CM_BUILD_NUMBER);

    private static final String CDH_KEY = "CDH";

    private static final String V_7_2_2 = "7.2.2";

    @InjectMocks
    private TargetImageAwareMixedPackageVersionService underTest;

    @Mock
    private MixedPackageMessageProvider mixedPackageMessageProvider;

    @Mock
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private MixedPackageNotificationService mixedPackageNotificationService;

    @Test
    void explicitCmAndAdditionalParcelVersionsAreComparedWithoutCatalogMetadata() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName(CDH_KEY).withVersion(V_7_2_2);
        ClouderaManagerProduct flink = new ClouderaManagerProduct().withName("FLINK").withVersion("1.20");
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion(CM_VERSION).withBuildNumber(CM_BUILD_NUMBER);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                V_7_2_2, "base", OsType.RHEL8, "x86_64", cdh, Set.of(flink), repo);
        Map<String, String> products = Map.of(CDH_KEY, V_7_2_2, "FLINK", "1.20");
        Set<ParcelInfo> active = createParcelInfo(products);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, products, CM_VERSION_AND_BUILD, active))
                .thenReturn(true);

        underTest.examinePackageVersionsWithTargetProperties(STACK_ID, properties, CM_VERSION_AND_BUILD, active);

        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, products, CM_VERSION_AND_BUILD, active);
        verifyNoInteractions(clouderaManagerProductTransformer, mixedPackageNotificationService);
    }

    @Test
    void missingTargetCmRepoSkipsComparison() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion(V_7_2_2);

        underTest.examinePackageVersionsWithTargetProperties(STACK_ID, properties, CM_VERSION_AND_BUILD, Set.of());

        verifyNoInteractions(mixedPackageVersionComparator, mixedPackageNotificationService);
    }

    @Test
    void missingTargetParcelVersionSkipsComparison() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName(CDH_KEY);
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion(CM_VERSION).withBuildNumber(CM_BUILD_NUMBER);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                V_7_2_2, "base", OsType.RHEL8, "x86_64", cdh, Set.of(), repo);

        underTest.examinePackageVersionsWithTargetProperties(STACK_ID, properties, CM_VERSION_AND_BUILD, Set.of());

        verifyNoInteractions(mixedPackageVersionComparator, mixedPackageNotificationService);
    }

    @Test
    void conflictingTargetParcelVersionsSkipComparison() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName(CDH_KEY).withVersion(V_7_2_2);
        ClouderaManagerProduct duplicate = new ClouderaManagerProduct().withName(CDH_KEY).withVersion("7.3.2");
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion(CM_VERSION).withBuildNumber(CM_BUILD_NUMBER);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                V_7_2_2, "base", OsType.RHEL8, "x86_64", cdh, Set.of(duplicate), repo);

        underTest.examinePackageVersionsWithTargetProperties(STACK_ID, properties, CM_VERSION_AND_BUILD, Set.of());

        verifyNoInteractions(mixedPackageVersionComparator, mixedPackageNotificationService);
    }

    @Test
    void duplicateParcelNamesWithSameVersionAreComparedOnce() {
        ClouderaManagerProduct cdh = new ClouderaManagerProduct().withName(CDH_KEY).withVersion(V_7_2_2).withParcel("https://first/parcels");
        ClouderaManagerProduct duplicate = new ClouderaManagerProduct().withName(CDH_KEY).withVersion(V_7_2_2).withParcel("https://second/parcels");
        ClouderaManagerRepo repo = new ClouderaManagerRepo().withVersion(CM_VERSION).withBuildNumber(CM_BUILD_NUMBER);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                V_7_2_2, "base", OsType.RHEL8, "x86_64", cdh, Set.of(duplicate), repo);
        Map<String, String> products = Map.of(CDH_KEY, V_7_2_2);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, products, CM_VERSION_AND_BUILD, Set.of()))
                .thenReturn(true);

        underTest.examinePackageVersionsWithTargetProperties(STACK_ID, properties, CM_VERSION_AND_BUILD, Set.of());

        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, products, CM_VERSION_AND_BUILD, Set.of());
    }

    @Test
    void testExaminePackageVersionsWithTargetImageShouldNotSendNotificationWhenThePackageVersionsAreValidAndEqualsWithTheTargetImage() {
        Set<ParcelInfo> activeParcels = createParcelInfo(Map.of(CDH_KEY, V_7_2_2));
        Image targetImage = createTargetImage();
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2);

        when(clouderaManagerProductTransformer.transformToMap(targetImage, true, true)).thenReturn(targetProducts);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels))
                .thenReturn(true);

        underTest.examinePackageVersionsWithTargetImage(STACK_ID, targetImage, CM_VERSION, activeParcels);

        verify(clouderaManagerProductTransformer).transformToMap(targetImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels);
        verifyNoInteractions(mixedPackageMessageProvider);
        verifyNoInteractions(mixedPackageNotificationService);
    }

    @Test
    void testExaminePackageVersionsWithTargetImageShouldSendNotificationWhenTheCdhPackageVersionIsHigherThanTheTarget() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, "7.2.9");
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        Image targetImage = createTargetImage();
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2);

        when(clouderaManagerProductTransformer.transformToMap(targetImage, true, true)).thenReturn(targetProducts);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels))
                .thenReturn(false);
        when(mixedPackageVersionComparator.getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, CM_VERSION))
                .thenReturn(activeProducts);
        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.9");
        when(mixedPackageMessageProvider.createMessageFromMap(activeProducts)).thenReturn("CDH 7.2.9");
        when(mixedPackageMessageProvider.createMessageFromMap(targetProducts)).thenReturn("CDH 7.2.2");
        when(mixedPackageVersionComparator.filterTargetPackageVersionsByNewerPackageVersions(targetProducts, CM_VERSION_AND_BUILD, activeProducts))
                .thenReturn(targetProducts);

        underTest.examinePackageVersionsWithTargetImage(STACK_ID, targetImage, CM_VERSION, activeParcels);

        verify(clouderaManagerProductTransformer).transformToMap(targetImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels);
        verify(mixedPackageVersionComparator).getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, CM_VERSION);
        verify(mixedPackageMessageProvider).createActiveParcelsMessage(activeParcels);
        verify(mixedPackageMessageProvider).createMessageFromMap(activeProducts);
        verify(mixedPackageMessageProvider).createMessageFromMap(targetProducts);
        verify(mixedPackageNotificationService).sendNotification(STACK_ID, STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED,
                List.of(CM_VERSION, "CDH 7.2.9", "CDH 7.2.9", "CDH 7.2.2"));
    }

    @Test
    void testExaminePackageVersionsWithTargetImageShouldSendNotificationWhenTheClouderaManagerVersionIsHigherThanTheTarget() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        Image targetImage = createTargetImage();
        Map<String, String> targetProducts = Map.of(CDH_KEY, V_7_2_2);
        String activeCmVersion = "7.4.0";
        Map<String, String> newerComponents = Map.of(CM.getDisplayName(), activeCmVersion);

        when(clouderaManagerProductTransformer.transformToMap(targetImage, true, true)).thenReturn(targetProducts);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, activeCmVersion, activeParcels))
                .thenReturn(false);
        when(mixedPackageVersionComparator.getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, activeCmVersion))
                .thenReturn(newerComponents);
        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.2");
        when(mixedPackageMessageProvider.createMessageFromMap(newerComponents)).thenReturn("Cloudera Manager 7.4.0");
        when(mixedPackageMessageProvider.createMessageFromMap(targetProducts)).thenReturn("Cloudera Manager 7.2.2");
        when(mixedPackageVersionComparator.filterTargetPackageVersionsByNewerPackageVersions(targetProducts, CM_VERSION_AND_BUILD, newerComponents))
                .thenReturn(targetProducts);

        underTest.examinePackageVersionsWithTargetImage(STACK_ID, targetImage, activeCmVersion, activeParcels);

        verify(clouderaManagerProductTransformer).transformToMap(targetImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, activeCmVersion, activeParcels);
        verify(mixedPackageVersionComparator).getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, activeCmVersion);
        verify(mixedPackageMessageProvider).createActiveParcelsMessage(activeParcels);
        verify(mixedPackageMessageProvider).createMessageFromMap(activeProducts);
        verify(mixedPackageMessageProvider).createMessageFromMap(targetProducts);
        verify(mixedPackageVersionComparator).filterTargetPackageVersionsByNewerPackageVersions(targetProducts, CM_VERSION_AND_BUILD, newerComponents);
        verify(mixedPackageNotificationService).sendNotification(STACK_ID, STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED,
                List.of(activeCmVersion, "CDH 7.2.2", "Cloudera Manager 7.4.0", "Cloudera Manager 7.2.2"));
    }

    @Test
    void testExaminePackageVersionsWithTargetImageShouldSendNotificationWhenTheCdhVersionIsNotValidAndTheTargetImageIsPresent() {
        Map<String, String> activeProducts = Map.of(CDH_KEY, V_7_2_2);
        Set<ParcelInfo> activeParcels = createParcelInfo(activeProducts);
        Image targetImage = createTargetImage();
        Map<String, String> targetProducts = Map.of(CDH_KEY, "7.2.9");

        when(clouderaManagerProductTransformer.transformToMap(targetImage, true, true)).thenReturn(targetProducts);
        when(mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels))
                .thenReturn(false);
        when(mixedPackageVersionComparator.getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, CM_VERSION))
                .thenReturn(Collections.emptyMap());
        when(mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels)).thenReturn("CDH 7.2.2");
        when(mixedPackageMessageProvider.createSuggestedVersionsMessage(targetProducts, activeParcels, CM_VERSION_AND_BUILD))
                .thenReturn("Cloudera Manager 7.2.0, CDH 7.2.9");

        underTest.examinePackageVersionsWithTargetImage(STACK_ID, targetImage, CM_VERSION, activeParcels);

        verify(clouderaManagerProductTransformer).transformToMap(targetImage, true, true);
        verify(mixedPackageVersionComparator).areAllComponentVersionsMatchingWithImage(CM_VERSION_AND_BUILD, targetProducts, CM_VERSION, activeParcels);
        verify(mixedPackageVersionComparator).getComponentsWithNewerVersionThanTheTarget(targetProducts, CM_VERSION_AND_BUILD, activeParcels, CM_VERSION);
        verify(mixedPackageMessageProvider).createActiveParcelsMessage(activeParcels);
        verify(mixedPackageNotificationService).sendNotification(STACK_ID, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED,
                List.of(CM_VERSION, "CDH 7.2.2", "Cloudera Manager 7.2.0, CDH 7.2.9"));
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createTargetImage() {
        Map<String, String> packageVersions = Map.of(CM.getKey(), CM_VERSION, ImagePackageVersion.CM_BUILD_NUMBER.getKey(), CM_BUILD_NUMBER);
        return Image.builder()
                .withPackageVersions(packageVersions)
                .build();
    }

    private Set<ParcelInfo> createParcelInfo(Map<String, String> parcels) {
        return parcels.entrySet().stream().map(entry -> new ParcelInfo(entry.getKey(), entry.getValue(), ParcelStatus.ACTIVATED)).collect(Collectors.toSet());
    }

}