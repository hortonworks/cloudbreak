package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class UpgradeServiceTest {

    private static final String CLUSTER_NAME = "cluster-name";

    private static final String CLUSTER_CRN = "cluster-crn";

    private static final long WORKSPACE_ID = 0L;

    private static final NameOrCrn OF_NAME = NameOrCrn.ofName(CLUSTER_NAME);

    private static final NameOrCrn OF_CRN = NameOrCrn.ofName(CLUSTER_CRN);

    private static final String IMAGE_ID = "imageId";

    @Mock
    private StackDtoService stackDtoService;

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
    private ComponentVersionProvider componentVersionProvider;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private UpgradeService underTest;

    @Mock
    private User user;

    @Mock
    private EntitlementService entitlementService;

    @Captor
    private ArgumentCaptor<ImageSettingsV4Request> captor;

    @Test
    public void shouldReturnNewImageName() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        Blueprint blueprint = new Blueprint();
        setUpMocks(image, true, "id-1", "id-2");
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);
        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(blueprintService.getByClusterId(any())).thenReturn(Optional.of(blueprint));

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(
                        eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq("AWS"),
                        eq(blueprint), eq(false), eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageName()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo(null);
    }

    @Test
    public void shouldReturnNoNewImageAndTryUseBaseImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");

        setUpMocksWithOutAttachedClusters(image, true, "id-1", "id-1");

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);
        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verifyNoMoreInteractions(distroXV1Endpoint);
        verifyNoMoreInteractions(componentConfigProviderService);
        verifyNoMoreInteractions(imageService);
        assertThat(result.getUpgrade()).isEqualTo(null);
        assertThat(result.getReason()).isEqualTo("According to the image catalog, the current image id-1 is already the latest version.");
    }

    @Test
    public void shouldNotAllowUpgradeIfDryRunFails() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");

        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StatedImage currentImageFromCatalog = imageFromCatalog(true, image.getImageId());
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(currentImageFromCatalog);

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.error(RepairValidation.of(List.of("Repair cannot be performed because there is an active flow running.",
                        "No external Database")));
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verifyNoMoreInteractions(distroXV1Endpoint);
        verifyNoMoreInteractions(componentConfigProviderService);
        verifyNoMoreInteractions(imageService);
        assertThat(result.getUpgrade()).isEqualTo(null);
        assertThat(result.getReason()).isEqualTo("Repair cannot be performed because there is an active flow running.; No external Database");
    }

    @Test
    public void upgradeNotAllowedWhenConnectedDataHubStackIsAvailable() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        Blueprint blueprint = new Blueprint();
        setUpMocks(image, true, "id-1", "id-2");

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.AVAILABLE);
        when(distroXV1Endpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses(Set.of(stackViewV4Response)));
        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(blueprintService.getByClusterId(any())).thenReturn(Optional.of(blueprint));

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq("AWS"), eq(blueprint), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageName()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo("Please stop connected DataHub clusters before upgrade.");
    }

    @Test
    public void upgradeNotAllowedWhenConnectedDataHubClusterIsAvailable() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        Blueprint blueprint = new Blueprint();
        setUpMocks(image, true, "id-1", "id-2");

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.STOPPED);
        ClusterViewV4Response clusterViewV4Response = new ClusterViewV4Response();
        clusterViewV4Response.setStatus(Status.AVAILABLE);
        stackViewV4Response.setCluster(clusterViewV4Response);
        when(distroXV1Endpoint.list(any(), anyString())).thenReturn(new StackViewV4Responses(Set.of(stackViewV4Response)));
        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(blueprintService.getByClusterId(any())).thenReturn(Optional.of(blueprint));

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq("AWS"), eq(blueprint), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageName()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo("Please stop connected DataHub clusters before upgrade.");
    }

    @Test
    public void upgradeAllowedWhenConnectedDataHubClusterIsAvailableAndEntitlementTrue()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(entitlementService.isUpgradeAttachedDatahubsCheckSkipped(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        Stack stack = getStack();
        Image image = getImage("id-1");
        Blueprint blueprint = new Blueprint();
        mockClouderaManagerRepoDetails();
        setupImageCatalogMocks(image, true, "id-1", "id-2");

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);

        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(blueprintService.getByClusterId(any())).thenReturn(Optional.of(blueprint));

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq("AWS"), eq(blueprint), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageName()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo(null);
    }

    @Test
    public void upgradeAllowedWhenConnectedDataHubStackAndClusterStoppedOrDeleted() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = getStack();
        Image image = getImage("id-1");
        Blueprint blueprint = new Blueprint();
        setUpMocks(image, true, "id-1", "id-2");

        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> repairStartResult =
                Result.success(new HashMap<>());
        when(clusterRepairService.repairWithDryRun(1L)).thenReturn(repairStartResult);

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
        when(stackDtoService.getStackViewByNameOrCrn(OF_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(blueprintService.getByClusterId(any())).thenReturn(Optional.of(blueprint));

        UpgradeOptionV4Response result = underTest.getOsUpgradeOptionByStackNameOrCrn(ACCOUNT_ID, OF_NAME, user);

        verify(clusterRepairService).repairWithDryRun(eq(stack.getId()));
        verify(distroXV1Endpoint).list(eq(null), eq("env-crn"));
        verify(componentConfigProviderService).getImage(1L);
        verify(imageService)
                .determineImageFromCatalog(eq(WORKSPACE_ID), captor.capture(), eq("aws"), eq("AWS"), eq(blueprint), eq(false),
                        eq(false), eq(user), any());
        assertThat(result.getUpgrade().getImageName()).isEqualTo("id-2");
        assertThat(result.getReason()).isEqualTo(null);
    }

    @Test
    public void testPrepareUpgradeLockedComponents() {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.TRUE);

        assertThrows(BadRequestException.class, () -> underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID));
        verifyNoInteractions(flowManager);
    }

    @Test
    public void testPrepareUpgradeImageNotFound() throws CloudbreakImageNotFoundException {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.FALSE);
        when(componentConfigProviderService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("nope"));

        assertThrows(NotFoundException.class, () -> underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID));
        verifyNoInteractions(flowManager);
    }

    @Test
    public void testPrepareUpgradeTriggersFlow() throws CloudbreakImageNotFoundException {
        StackDto stack = getStackDto();
        when(stackDtoService.getByNameOrCrn(OF_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, IMAGE_ID)).thenReturn(Boolean.FALSE);
        Image image = mock(Image.class);
        String imgCatName = "imgCatName";
        when(image.getImageCatalogName()).thenReturn(imgCatName);
        String imgCatUrl = "imgCatUrl";
        when(image.getImageCatalogUrl()).thenReturn(imgCatUrl);
        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(image);
        ArgumentCaptor<ImageChangeDto> imageChangeDtoArgumentCaptor = ArgumentCaptor.forClass(ImageChangeDto.class);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollId");
        when(flowManager.triggerClusterUpgradePreparation(eq(stack.getId()), imageChangeDtoArgumentCaptor.capture())).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.prepareClusterUpgrade(ACCOUNT_ID, OF_CRN, IMAGE_ID);

        assertEquals(flowIdentifier, result);
        ImageChangeDto imageChangeDto = imageChangeDtoArgumentCaptor.getValue();
        assertEquals(imgCatName, imageChangeDto.getImageCatalogName());
        assertEquals(imgCatUrl, imageChangeDto.getImageCatalogUrl());
        assertEquals(IMAGE_ID, imageChangeDto.getImageId());
        assertEquals(stack.getId(), imageChangeDto.getStackId());
    }

    private void mockClouderaManagerRepoDetails() {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("cm-base-url");
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(1L)).thenReturn(clouderaManagerRepo);
    }

    private void setUpMocks(Image image, boolean prewarmedImage, String oldImage, String newImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupAttachedClusterMocks();
        mockClouderaManagerRepoDetails();
        setupImageCatalogMocks(image, prewarmedImage, oldImage, newImage);
    }

    private void setUpMocksWithOutAttachedClusters(Image image, boolean prewarmedImage, String oldImage, String newImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        mockClouderaManagerRepoDetails();
        setupImageCatalogMocks(image, prewarmedImage, oldImage, newImage);
    }

    private void setupImageCatalogMocks(Image image, boolean prewarmedImage, String oldImage, String newImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(image);
        StatedImage currentImageFromCatalog = imageFromCatalog(prewarmedImage, oldImage);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(currentImageFromCatalog);
        StatedImage latestImage = imageFromCatalog(true, newImage);
        when(imageService.determineImageFromCatalog(anyLong(), any(), anyString(), any(), any(), anyBoolean(),
                anyBoolean(), any(), any())).thenReturn(latestImage);
    }

    public void setupAttachedClusterMocks() {
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        stackViewV4Responses.setResponses(List.of());
        when(distroXV1Endpoint.list(eq(null), anyString())).thenReturn(stackViewV4Responses);
    }

    private Image getImage(String imageName) {
        return ModelImageTestBuilder.builder()
                .withImageName(imageName)
                .withOs("os")
                .withImageCatalogName("catalogName")
                .withImageCatalogUrl("catalogUrl")
                .withImageId("id-1")
                .build();
    }

    private StatedImage imageFromCatalog(boolean prewarmed, String imageName) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        lenient().when(image.isPrewarmed()).thenReturn(prewarmed);
        lenient().when(image.getUuid()).thenReturn("uuid");
        lenient().when(image.getImageSetsByProvider()).thenReturn(Map.of("aws", Map.of("eu-central-1", imageName)));
        return StatedImage.statedImage(image, null, null);
    }

    private StackDto getStackDto() {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = getStack();
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getWorkspace()).thenReturn(stack.getWorkspace());
        return stackDto;
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
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        return stack;
    }

}