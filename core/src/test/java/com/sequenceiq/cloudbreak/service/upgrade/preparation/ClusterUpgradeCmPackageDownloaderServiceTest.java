package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeCmPackageDownloaderServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterUpgradeCmPackageDownloaderService underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ClusterManagerUpgradePreparationStateParamsProvider clusterManagerUpgradePreparationStateParamsProvider;

    @Mock
    private StackDto stackDto;

    @BeforeEach
    public void before() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setId(STACK_ID);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
    }

    @Test
    void testDownloadCmPackagesSkipPackageDownload() throws Exception {
        ClouderaManagerRepo currentRepo = new ClouderaManagerRepo().withBuildNumber("123");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID)).thenReturn(currentRepo);
        ClusterUpgradeProperties properties = properties("123");

        underTest.downloadCmPackages(STACK_ID, properties);

        verifyNoInteractions(eventService, hostOrchestrator);
    }

    private ClusterUpgradeProperties properties(String buildNumber) {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts("7.3.2", "base-image", OsType.RHEL8, "x86_64",
                null, Set.of(), new ClouderaManagerRepo().withVersion("7.13.1").withBuildNumber("different-top-level-build"));
        ClusterUpgradeProperties.TargetImageUpgradeContext target = properties.targetImage();
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), buildNumber);
        return new ClusterUpgradeProperties(properties.options(), properties.currentImage(), new ClusterUpgradeProperties.TargetImageUpgradeContext(
                target.imageId(), target.catalogName(), target.catalogUrl(), target.runtimeVersion(), target.imageVersion(), target.cdhBuildNumber(),
                packageVersions, target.tags(), target.osType(), target.os(), target.architecture(), target.date(), target.created(), target.imageName(),
                target.stackDetails(), target.repo(), target.preWarmParcelEntries(), target.preWarmCsd(), target.cdhParcel(), target.preWarmParcels(),
                target.clouderaManagerRepo()));
    }

    @Test
    void testDownloadCmPackagesWhenCurrentBuildNumberIsNull() throws Exception {
        ClouderaManagerRepo currentRepo = new ClouderaManagerRepo().withBuildNumber(null);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID)).thenReturn(currentRepo);
        ClusterUpgradeProperties properties = properties("124");
        when(clusterManagerUpgradePreparationStateParamsProvider.createParamsForCmPackageDownload(properties)).thenReturn(Map.of());
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, "cloudera/repo/upgrade-preparation", 200, 3))
                .thenReturn(mock(OrchestratorStateParams.class));

        underTest.downloadCmPackages(STACK_ID, properties);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_CM_PACKAGES);
        verify(clusterHostServiceRunner).redeployStates(stackDto);
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void testSkipDownloadWhenBothBuildNumbersAreEqualIncludingMissingValues(String buildNumber) throws Exception {
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID))
                .thenReturn(new ClouderaManagerRepo().withBuildNumber(buildNumber));

        underTest.downloadCmPackages(STACK_ID, properties(buildNumber));

        verifyNoInteractions(eventService, hostOrchestrator, clusterHostServiceRunner, clusterManagerUpgradePreparationStateParamsProvider);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"124", " "})
    void testDownloadCmPackagesWhenBuildNumbersDiffer(String candidateBuildNumber) throws Exception {
        ClouderaManagerRepo currentRepo = new ClouderaManagerRepo().withBuildNumber("123");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID)).thenReturn(currentRepo);
        ClusterUpgradeProperties properties = properties(candidateBuildNumber);
        when(clusterManagerUpgradePreparationStateParamsProvider.createParamsForCmPackageDownload(properties)).thenReturn(Map.of());
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, "cloudera/repo/upgrade-preparation", 200, 3))
                .thenReturn(mock(OrchestratorStateParams.class));

        underTest.downloadCmPackages(STACK_ID, properties);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_CM_PACKAGES);
        verify(clusterHostServiceRunner).redeployStates(stackDto);
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));
    }
}