package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackUpgradeServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String STACK_NAME = "test-stack";

    private static final String CATALOG_URL = "/images";

    private static final String CURRENT_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    @InjectMocks
    private StackUpgradeService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private ImageService imageService;

    @Mock
    private StackUpgradeImageFilter stackUpgradeImageFilter;

    @Mock
    private UpgradeOptionsResponseFactory upgradeOptionsResponseFactory;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Test
    public void testCheckForUpgradesByNameShouldReturnsImagesWhenThereAreAvailableImages()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE));
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Result result = Mockito.mock(Result.class);
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV2 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        UpgradeOptionsV4Response response = new UpgradeOptionsV4Response();

        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(clusterRepairService.checkRepairAll(stack)).thenReturn(result);
        when(result.isError()).thenReturn(false);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogProvider.getImageCatalogV2(CATALOG_URL)).thenReturn(imageCatalog);
        Images filteredImages = createFilteredImages(properImage);
        when(stackUpgradeImageFilter.filter(imageCatalog.getImages().getCdhImages(), imageCatalog.getVersions(), currentImageFromCatalog,
                stack.getCloudPlatform())).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeOptionsV4Response actual = underTest.checkForUpgradesByName(WORKSPACE_ID, STACK_NAME);

        assertEquals(response, actual);
        verify(stackService).getByNameInWorkspace(STACK_NAME, WORKSPACE_ID);
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogProvider).getImageCatalogV2(CATALOG_URL);
        verify(stackUpgradeImageFilter).filter(imageCatalog.getImages().getCdhImages(), imageCatalog.getVersions(), currentImageFromCatalog,
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

    private CloudbreakImageCatalogV2 createImageCatalog(List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> images) {
        return new CloudbreakImageCatalogV2(new Images(null, null, null, images, null), new Versions(Collections.emptyList()));
    }

    private Images createFilteredImages(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return new Images(null, null, null, List.of(image), null);
    }
}