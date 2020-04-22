package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeAvailabilityServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String STACK_NAME = "test-stack";

    private static final String CATALOG_URL = "/images";

    private static final String CURRENT_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    @InjectMocks
    private ClusterUpgradeAvailabilityService underTest;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackService stackService;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private ImageService imageService;

    @Mock
    private ClusterUpgradeImageFilter clusterUpgradeImageFilter;

    @Mock
    private UpgradeOptionsResponseFactory upgradeOptionsResponseFactory;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private GatewayConfig gatewayConfig;

    @Test
    public void testCheckForUpgradesByNameShouldReturnsImagesWhenThereAreAvailableImages()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE));
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Result result = Mockito.mock(Result.class);
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();

        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(clusterRepairService.checkRepairAll(stack)).thenReturn(result);
        when(result.isError()).thenReturn(false);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogProvider.getImageCatalogV3(CATALOG_URL)).thenReturn(imageCatalog);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        when(clusterUpgradeImageFilter.filter(imageCatalog.getImages().getCdhImages(), imageCatalog.getVersions(), currentImageFromCatalog,
                stack.getCloudPlatform())).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeOptionsV4Response actual = underTest.checkForUpgradesByName(WORKSPACE_ID, STACK_NAME);

        assertEquals(response, actual);
        verify(stackService).getByNameInWorkspace(STACK_NAME, WORKSPACE_ID);
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogProvider).getImageCatalogV3(CATALOG_URL);
        verify(clusterUpgradeImageFilter).filter(imageCatalog.getImages().getCdhImages(), imageCatalog.getVersions(), currentImageFromCatalog,
                stack.getCloudPlatform());
        verify(upgradeOptionsResponseFactory).createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnsEmptyListWhenCurrentImageIsNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE));
        Result result = Mockito.mock(Result.class);
        when(clusterRepairService.checkRepairAll(stack)).thenReturn(result);
        when(result.isError()).thenReturn(false);
        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(imageService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("Image not found."));

        UpgradeOptionsV4Response actual = underTest.checkForUpgradesByName(WORKSPACE_ID, STACK_NAME);

        assertNull(actual.getCurrent());
        assertNull(actual.getUpgradeCandidates());
        assertEquals("Failed to retrieve imaged due to Image not found.", actual.getReason());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnsEmptyListWhenTheClusterIsNotAvailable() {
        Stack stack = createStack(createStackStatus(Status.CREATE_FAILED));
        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

        UpgradeOptionsV4Response actual = underTest.checkForUpgradesByName(WORKSPACE_ID, STACK_NAME);

        assertNull(actual.getCurrent());
        assertNull(actual.getUpgradeCandidates());
        assertEquals("Cannot upgrade cluster because its in CREATE_FAILED state.", actual.getReason());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnsEmptyListWhenTheClusterIsNotRepairable() {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE));
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> result = mock(Result.class);
        RepairValidation repairValidation = mock(RepairValidation.class);
        when(clusterRepairService.checkRepairAll(stack)).thenReturn(result);
        when(result.isError()).thenReturn(true);
        String validationError = "External RDS is not attached.";
        when(result.getError()).thenReturn(repairValidation);
        when(repairValidation.getValidationErrors()).thenReturn(Collections.singletonList(validationError));
        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

        UpgradeOptionsV4Response actual = underTest.checkForUpgradesByName(WORKSPACE_ID, STACK_NAME);

        assertNull(actual.getCurrent());
        assertNull(actual.getUpgradeCandidates());
        assertEquals(validationError, actual.getReason());
    }

    @Test
    public void testRunningDataHubsAttached() {
        StackViewV4Response datahubStack1 = new StackViewV4Response();
        datahubStack1.setStatus(Status.AVAILABLE);
        datahubStack1.setName("stack-1");
        ClusterViewV4Response datahubCluster1 = new ClusterViewV4Response();
        datahubCluster1.setStatus(Status.AVAILABLE);
        datahubStack1.setCluster(datahubCluster1);
        StackViewV4Response datahubStack2 = new StackViewV4Response();
        datahubStack2.setStatus(Status.DELETE_COMPLETED);
        datahubStack2.setName("stack-2");
        ClusterViewV4Response datahubCluster2 = new ClusterViewV4Response();
        datahubCluster2.setStatus(Status.DELETE_COMPLETED);
        datahubStack2.setCluster(datahubCluster2);
        StackViewV4Response datahubStack3 = new StackViewV4Response();
        datahubStack3.setStatus(Status.STOPPED);
        datahubStack3.setName("stack-3");
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(datahubStack1, datahubStack2, datahubStack3));
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();
        UpgradeOptionsV4Response actual = underTest.checkForNotAttachedClusters(stackViewV4Responses, response);

        assertNull(actual.getUpgradeCandidates());
        assertEquals("There are attached Data Hub clusters in incorrect state: stack-1. Please stop those to be able to perform the upgrade.",
                actual.getReason());

    }

    @Test
    public void testNotRunningDataHubsAttached() {
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
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of(datahubStack1, datahubStack2, datahubStack3));
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();
        UpgradeOptionsV4Response actual = underTest.checkForNotAttachedClusters(stackViewV4Responses, response);

        assertNull(actual.getReason());
    }

    @Test
    public void tesDataHubsNotAttached() {

        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses(Set.of());
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();
        UpgradeOptionsV4Response actual = underTest.checkForNotAttachedClusters(stackViewV4Responses, response);

        assertNull(actual.getReason());
    }

    @Test
    public void testSaltStatesArePresent() throws TransactionService.TransactionExecutionException {
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();
        when(transactionService.required(any(Supplier.class))).thenReturn(gatewayConfig);
        UpgradeOptionsV4Response actual = underTest.checkIfClusterUpgradable(WORKSPACE_ID, STACK_NAME, response);
        assertNull(actual.getReason());
    }

    @Test
    public void testSaltStatesAreMissing() throws CloudbreakOrchestratorFailedException {

        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();

        doThrow(new CloudbreakOrchestratorFailedException("Cluster is not upgradeable due to required Salt files not being present. "
                        + "Please ensure that your cluster is up to date!"))
                .when(hostOrchestrator).
                checkIfClusterUpgradable(any());

        UpgradeOptionsV4Response actual = underTest.checkIfClusterUpgradable(WORKSPACE_ID, STACK_NAME, response);

        assertNotNull(actual.getReason());
        assertEquals("Cluster is not upgradeable due to required Salt files not being present. "
                + "Please ensure that your cluster is up to date!", actual.getReason());
    }

    private StackStatus createStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return stackStatus;
    }

    private Stack createStack(StackStatus stackStatus) {
        Stack stack = new Stack();
        stack.setId(2L);
        stack.setCloudPlatform("AWS");
        stack.setStackStatus(stackStatus);
        return stack;
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, null, null, CATALOG_URL, null, CURRENT_IMAGE_ID, null);
    }

    private CloudbreakImageCatalogV3 createImageCatalog(List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> images) {
        return new CloudbreakImageCatalogV3(new Images(null, null, null, images, null), new Versions(Collections.emptyList()));
    }

    private ImageFilterResult createFilteredImages(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return new ImageFilterResult(new Images(null, null, null, List.of(image), null), null);
    }
}