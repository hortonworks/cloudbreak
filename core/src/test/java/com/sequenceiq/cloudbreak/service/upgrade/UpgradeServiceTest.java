package com.sequenceiq.cloudbreak.service.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@ExtendWith(MockitoExtension.class)
public class UpgradeServiceTest {

    public static final String CLUSTER_NAME = "cluster-name";

    private static final long WORKSPACE_ID = 0L;

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private UpgradeService underTest;

    @Mock
    private User user;

    @Captor
    private ArgumentCaptor<ImageSettingsV4Request> captor;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void shouldReturnNewImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        setUpMocks(stack, image, true, "id-1", "id-2");

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        UpgradeOptionV4Response result = underTest.getUpgradeOptionByStackName(WORKSPACE_ID, CLUSTER_NAME, user);

        verify(stackService).findStackByNameAndWorkspaceId(eq(CLUSTER_NAME), eq(WORKSPACE_ID));
        verify(clusterRepairService).canRepairAll(eq(stack));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(
                        eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq(stack.getCluster().getBlueprint()), eq(false),  eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageId()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo(null);
    }

    @Test
    public void shouldReturnNoNewImageAndTryUseBaseImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        when(stackService.findStackByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(stack));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StatedImage currentImageFromCatalog = imageFromCatalog(false, "id-1");
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(currentImageFromCatalog);

        UpgradeOptionV4Response result = underTest.getUpgradeOptionByStackName(WORKSPACE_ID, CLUSTER_NAME, user);

        verify(stackService).findStackByNameAndWorkspaceId(eq(CLUSTER_NAME), eq(WORKSPACE_ID));
        verify(clusterRepairService).canRepairAll(stack);
        verifyNoMoreInteractions(distroXV1Endpoint);
        verifyZeroInteractions(componentConfigProviderService);
        verifyZeroInteractions(imageService);
        assertThat(result.getUpgrade()).isEqualTo(null);
    }

    @Test
    public void upgradeNotAllowedWhenConnectedDataHubStackIsAvailable() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        setUpMocks(stack, image, true, "id-1", "id-2");

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.AVAILABLE);
        when(distroXV1Endpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses(Set.of(stackViewV4Response)));

        UpgradeOptionV4Response result = underTest.getUpgradeOptionByStackName(WORKSPACE_ID, CLUSTER_NAME, user);

        verify(stackService).findStackByNameAndWorkspaceId(eq(CLUSTER_NAME), eq(WORKSPACE_ID));
        verify(clusterRepairService).canRepairAll(eq(stack));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq(stack.getCluster().getBlueprint()), eq(false),
                        eq(false),  eq(user), any());
        assertThat(result.getUpgrade().getImageId()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo("Please stop connected DataHub clusters before upgrade.");
    }

    @Test
    public void upgradeNotAllowedWhenConnectedDataHubClusterIsAvailable() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        setUpMocks(stack, image, true, "id-1", "id-2");

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.STOPPED);
        ClusterViewV4Response clusterViewV4Response = new ClusterViewV4Response();
        clusterViewV4Response.setStatus(Status.AVAILABLE);
        stackViewV4Response.setCluster(clusterViewV4Response);
        when(distroXV1Endpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses(Set.of(stackViewV4Response)));

        UpgradeOptionV4Response result = underTest.getUpgradeOptionByStackName(WORKSPACE_ID, CLUSTER_NAME, user);

        verify(stackService).findStackByNameAndWorkspaceId(eq(CLUSTER_NAME), eq(WORKSPACE_ID));
        verify(clusterRepairService).canRepairAll(eq(stack));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq(stack.getCluster().getBlueprint()), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageId()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo("Please stop connected DataHub clusters before upgrade.");
    }

    @Test
    public void upgradeAllowedWhenConnectedDataHubStackAndClusterStoppedOrDeleted() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        setUpMocks(stack, image, true, "id-1", "id-2");

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);

        StackViewV4Response datahubStack1 = new StackViewV4Response();
        datahubStack1.setStatus(Status.STOPPED);
        ClusterViewV4Response datahubCluster1 = new ClusterViewV4Response();
        datahubCluster1.setStatus(Status.STOPPED);
        datahubStack1.setCluster(datahubCluster1);
        StackViewV4Response datahubStack2 = new StackViewV4Response();
        datahubStack2.setStatus(Status.DELETE_COMPLETED);
        ClusterViewV4Response datahubCluster2 = new ClusterViewV4Response();
        datahubCluster2.setStatus(Status.DELETE_COMPLETED);
        datahubStack2.setCluster(datahubCluster2);
        StackViewV4Response datahubStack3 = new StackViewV4Response();
        datahubStack3.setStatus(Status.STOPPED);
        when(distroXV1Endpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses(Set.of(datahubStack1, datahubStack2, datahubStack3)));

        UpgradeOptionV4Response result = underTest.getUpgradeOptionByStackName(WORKSPACE_ID, CLUSTER_NAME, user);

        verify(stackService).findStackByNameAndWorkspaceId(eq(CLUSTER_NAME), eq(WORKSPACE_ID));
        verify(clusterRepairService).canRepairAll(eq(stack));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq(stack.getCluster().getBlueprint()), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageId()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo(null);
    }

    private void setUpMocks(Stack stack, Image image, boolean prewarmedImage, String oldImage, String newImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(clusterRepairService.canRepairAll(stack)).thenReturn(true);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        stackViewV4Responses.setResponses(List.of());
        when(distroXV1Endpoint.list(eq(null), anyString())).thenReturn(stackViewV4Responses);
        when(stackService.findStackByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(stack));
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StatedImage currentImageFromCatalog = imageFromCatalog(prewarmedImage, oldImage);
        when(imageCatalogService.getImage(anyString(), anyString(), anyString())).thenReturn(currentImageFromCatalog);
        StatedImage latestImage = imageFromCatalog(true, newImage);
        when(imageService.determineImageFromCatalog(anyLong(), any(), anyString(), any(), anyBoolean(), anyBoolean(),  any(), any())).thenReturn(latestImage);
    }

    private Image getImage(String imageId) {
        return new Image(
                null,
                null,
                "os",
                null,
                "catalogUrl",
                "catalogName",
                imageId,
                Map.of()
        );
    }

    private StatedImage imageFromCatalog(boolean prewarmed, String imageId) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        lenient().when(image.isPrewarmed()).thenReturn(prewarmed);
        lenient().when(image.getUuid()).thenReturn(imageId);
        lenient().when(image.getImageSetsByProvider()).thenReturn(Map.of("aws", Map.of("eu-central-1", "ami-1234")));
        StatedImage statedImage = StatedImage.statedImage(image, null, null);
        return statedImage;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn("env-crn");
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant("AWS");
        stack.setRegion("eu-central-1");
        Blueprint blueprint = new Blueprint();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        return stack;
    }

}