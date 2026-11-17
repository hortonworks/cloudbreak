package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.UpgradeRequestOptions;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.ServiceUpgradeValidationRequestTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class LockedComponentsValidatorTest {

    private static final String OLD_CDH = "7.3.2-1.cdh7.3.2.p0.77083870";

    private static final String TARGET_CDH = "7.3.2-1.cdh7.3.2.p20000.83281140";

    private static final String TARGET_CM = "7.13.2.20000-83309838";

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private StackDto stack;

    @InjectMocks
    private LockedComponentsValidator underTest;

    @ParameterizedTest
    @ValueSource(strings = {TARGET_CDH, OLD_CDH})
    void rejectsOldActiveRuntimeRegardlessOfWhetherTheDatabaseWasResynced(String recordedCdh) {
        givenDataHubWithRecordedParcels(Map.of("CDH", recordedCdh));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(
                activeParcel("CDH", OLD_CDH), new ParcelInfo("CDH", TARGET_CDH, ParcelStatus.DISTRIBUTED)));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of())))
                .isInstanceOf(UpgradeValidationFailedException.class)
                .hasMessageContaining("target parcels are not active").hasMessageContaining(TARGET_CDH);
    }

    @Test
    void requiresActiveCdhEvenWhenItIsMissingFromTheRecordedComponents() {
        givenDataHubWithRecordedParcels(Map.of("NIFI", "2.0"));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(activeParcel("NIFI", "2.0")));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of(product("NIFI", "2.0")))))
                .isInstanceOf(UpgradeValidationFailedException.class).hasMessageContaining(TARGET_CDH);
    }

    @Test
    void rejectsRequiredParcelMissingFromTheTarget() {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH, "NIFI", "2.0"));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of())))
                .isInstanceOf(UpgradeValidationFailedException.class).hasMessageContaining("Cannot confirm the target versions");
        verifyNoInteractions(cmServerQueryService);
    }

    @Test
    void rejectsMissingActiveNonCdhParcel() {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH, "NIFI", "2.0"));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(activeParcel("CDH", TARGET_CDH)));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of(product("NIFI", "2.0")))))
                .isInstanceOf(UpgradeValidationFailedException.class).hasMessageContaining("NIFI 2.0");
    }

    @ParameterizedTest
    @CsvSource({"83309838," + TARGET_CM, "83309838p," + TARGET_CM, "83309838," + TARGET_CM + "p"})
    void allowsOsUpgradeWhenAllRequiredComponentsMatch(String targetCmBuild, String installedCm) {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH, "NIFI", "2.0"));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(
                activeParcel("CDH", TARGET_CDH), activeParcel("NIFI", "2.0"), activeParcel("CUSTOM", "1.0")));
        when(cmServerQueryService.queryCmServerVersion(stack)).thenReturn(Optional.of(installedCm));

        underTest.validate(lockedRequest(Set.of(product("NIFI", "2.0"), product("SPARK3", "3.5")), targetCmBuild));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "7.13.1-12345")
    void rejectsDifferentOrUnconfirmedCmVersion(String installedCm) {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(activeParcel("CDH", TARGET_CDH)));
        when(cmServerQueryService.queryCmServerVersion(stack)).thenReturn(Optional.ofNullable(installedCm));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of())))
                .isInstanceOf(UpgradeValidationFailedException.class).hasMessageContaining(TARGET_CM);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "false,true", "true,false"})
    void orderedBaseImageUpgradeRequiresTheRetainedParcelsAndCmBuild(boolean parcelMatches, boolean cmMatches) {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH, "NIFI", "2.0"));
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withCurrentProducts(
                ClusterUpgradePropertiesTestUtils.withFlags(true, false, true), Set.of(product("CDH", TARGET_CDH), product("NIFI", "2.0")),
                new ClouderaManagerRepo().withVersion("7.13.2.20000").withBuildNumber("83309838"));
        ServiceUpgradeValidationRequest request = ServiceUpgradeValidationRequestTestUtils.of(stack, properties);
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(activeParcel("CDH", parcelMatches ? TARGET_CDH : OLD_CDH),
                activeParcel("NIFI", "2.0")));
        if (parcelMatches) {
            when(cmServerQueryService.queryCmServerVersion(stack)).thenReturn(Optional.of(cmMatches ? TARGET_CM : "7.13.2.20000-1"));
        }

        if (parcelMatches && cmMatches) {
            underTest.validateOrderedOsUpgrade(request);
        } else {
            assertThatThrownBy(() -> underTest.validateOrderedOsUpgrade(request)).isInstanceOf(UpgradeValidationFailedException.class)
                    .hasMessageContaining(parcelMatches ? TARGET_CM : TARGET_CDH);
        }
    }

    @Test
    void rejectsTargetCmVersionWithoutABuildNumber() {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH));
        when(cmServerQueryService.queryAllParcels(stack)).thenReturn(Set.of(activeParcel("CDH", TARGET_CDH)));

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of(), null)))
                .isInstanceOf(UpgradeValidationFailedException.class).hasMessageContaining("Cloudera Manager version");
    }

    @Test
    void rejectsUpgradeIfTheParcelQueryFails() {
        givenDataHubWithRecordedParcels(Map.of("CDH", TARGET_CDH));
        IllegalStateException cause = new IllegalStateException("CM unavailable");
        when(cmServerQueryService.queryAllParcels(stack)).thenThrow(cause);

        assertThatThrownBy(() -> underTest.validate(lockedRequest(Set.of())))
                .isInstanceOf(UpgradeValidationFailedException.class).hasCause(cause);
    }

    @Test
    void leavesRuntimeUpgradesAlone() {
        underTest.validate(ServiceUpgradeValidationRequestTestUtils.of(stack, ClusterUpgradePropertiesTestUtils.withFlags(false, false, true)));

        verifyNoInteractions(cmServerQueryService, imageFilterParamsFactory);
    }

    @Test
    void leavesDataLakeUpgradesAlone() {
        when(stack.getType()).thenReturn(StackType.DATALAKE);

        underTest.validate(lockedRequest(Set.of()));

        verifyNoInteractions(cmServerQueryService, imageFilterParamsFactory);
    }

    private void givenDataHubWithRecordedParcels(Map<String, String> parcels) {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(parcels);
    }

    private ServiceUpgradeValidationRequest lockedRequest(Set<ClouderaManagerProduct> parcels) {
        return lockedRequest(parcels, "83309838");
    }

    private ServiceUpgradeValidationRequest lockedRequest(Set<ClouderaManagerProduct> parcels, String cmBuild) {
        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo().withVersion("7.13.2.20000").withBuildNumber(cmBuild);
        ClusterUpgradeProperties fixture = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "7.3.2", OsType.RHEL8, "x86_64",
                product("CDH", TARGET_CDH), parcels, cmRepo);
        ClusterUpgradeProperties properties = new ClusterUpgradeProperties(new UpgradeRequestOptions(true, true, false),
                fixture.currentImage(), fixture.targetImage());
        return ServiceUpgradeValidationRequestTestUtils.of(stack, properties);
    }

    private ClouderaManagerProduct product(String name, String version) {
        return new ClouderaManagerProduct().withName(name).withVersion(version);
    }

    private ParcelInfo activeParcel(String name, String version) {
        return new ParcelInfo(name, version, ParcelStatus.ACTIVATED);
    }
}
