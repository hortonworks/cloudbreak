package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeCmPackageDownloaderServiceTest {

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_ID = "image-id";

    @InjectMocks
    private ClusterUpgradeCmPackageDownloaderService underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ImageCatalogService imageCatalogService;

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

    @Mock
    private ImageService imageService;

    @BeforeEach
    public void before() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setId(STACK_ID);
        when(stackDto.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
    }

    @Test
    void testDownloadCmPackagesSkipPackageDownload() throws Exception {
        Image candidateImage = mock(Image.class);
        ClouderaManagerRepo currentRepo = new ClouderaManagerRepo().withBuildNumber("123");
        com.sequenceiq.cloudbreak.cloud.model.Image currentModelImage = createModelImage();

        when(imageService.getImage(STACK_ID)).thenReturn(currentModelImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, currentModelImage.getImageCatalogUrl(), currentModelImage.getImageCatalogName(), IMAGE_ID))
                .thenReturn(StatedImage.statedImage(candidateImage, null, null));
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID)).thenReturn(currentRepo);
        when(candidateImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER)).thenReturn("123");

        underTest.downloadCmPackages(STACK_ID, IMAGE_ID);

        verifyNoInteractions(eventService, hostOrchestrator);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createModelImage() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageCatalogName("catalog-name")
                .withImageCatalogUrl("catalog-url")
                .build();
    }

    @Test
    void testDownloadCmPackagesNoDownloadNeeded() throws Exception {
        Image candidateImage = mock(Image.class);
        ClouderaManagerRepo currentRepo = new ClouderaManagerRepo().withBuildNumber("123");
        com.sequenceiq.cloudbreak.cloud.model.Image currentModelImage = createModelImage();

        when(imageService.getImage(STACK_ID)).thenReturn(currentModelImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, currentModelImage.getImageCatalogUrl(), currentModelImage.getImageCatalogName(), IMAGE_ID))
                .thenReturn(StatedImage.statedImage(candidateImage, null, null));
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(STACK_ID)).thenReturn(currentRepo);
        when(candidateImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER)).thenReturn("124");
        when(clusterManagerUpgradePreparationStateParamsProvider.createParamsForCmPackageDownload(candidateImage, STACK_ID)).thenReturn(Map.of());
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, "cloudera/repo/upgrade-preparation", 200, 3))
                .thenReturn(mock(OrchestratorStateParams.class));

        underTest.downloadCmPackages(STACK_ID, IMAGE_ID);

        verify(eventService).fireCloudbreakEvent(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_CM_PACKAGES);
        verify(clusterHostServiceRunner).redeployStates(stackDto);
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));
    }
}