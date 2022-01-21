package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRepairService;
import com.sequenceiq.cloudbreak.service.cluster.model.HostGroupName;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.cluster.model.Result;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeAvailabilityServiceTest {

    private static final String ACCOUNT_ID = "cloudera";

    private static final String CATALOG_NAME = "catalog";

    private static final String CATALOG_URL = "/images";

    private static final String FALLBACK_CATALOG_URL = "/images/fallback";

    private static final String CURRENT_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String ANOTHER_IMAGE_ID = "another-image-id";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String ANOTHER_TARGET_RUNTIME = "7.2.0";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private static final Long WORKSPACE_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "region";

    private static final long STACK_ID = 2L;

    private static final InternalUpgradeSettings INTERNAL_UPGRADE_SETTINGS = new InternalUpgradeSettings(false, true, true);

    @InjectMocks
    private ClusterUpgradeAvailabilityService underTest;

    @Mock
    private ImageCatalogService imageCatalogService;

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
    private ClusterApi clusterApi;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private EntitlementService entitlementService;

    private boolean lockComponents;

    private final Map<String, String> activatedParcels = new HashMap<>();

    private String accountId = "account1";

    @Test
    public void testCheckForUpgradesByNameShouldReturnImagesWhenThereAreAvailableImagesUsingImageCatalogByName()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), StackType.WORKLOAD);
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Result result = Mockito.mock(Result.class);
        ImageCatalog imageCatalogDomain = createImageCatalogDomain();
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        ImageFilterParams imageFilterParams = createImageFilterParams(stack, currentImageFromCatalog);
        UpgradeV4Response response = new UpgradeV4Response();

        when(imageFilterParamsFactory.create(currentImageFromCatalog, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        when(clusterRepairService.repairWithDryRun(stack.getId())).thenReturn(result);
        when(result.isError()).thenReturn(false);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenReturn(imageCatalogDomain);
        when(imageCatalogProvider.getImageCatalogV3(CATALOG_URL)).thenReturn(imageCatalog);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertEquals(response, actual);
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogProvider).getImageCatalogV3(CATALOG_URL);
        verify(clusterUpgradeImageFilter).filter(accountId, imageCatalog, imageFilterParams);
        verify(upgradeOptionsResponseFactory).createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName());
    }

    @Test
    public void testCheckForUpgradesByNameShouldReturnImagesFromFallbackCatalogWhenThereAreAvailableImagesButImageCatalogByNameNotFound()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), StackType.WORKLOAD);
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Result result = Mockito.mock(Result.class);
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        ImageFilterParams imageFilterParams = createImageFilterParams(stack, currentImageFromCatalog);
        UpgradeV4Response response = new UpgradeV4Response();

        when(imageFilterParamsFactory.create(currentImageFromCatalog, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        when(clusterRepairService.repairWithDryRun(stack.getId())).thenReturn(result);
        when(result.isError()).thenReturn(false);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenThrow(new NotFoundException("not found"));
        when(imageCatalogProvider.getImageCatalogV3(FALLBACK_CATALOG_URL)).thenReturn(imageCatalog);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertEquals(response, actual);
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogProvider).getImageCatalogV3(FALLBACK_CATALOG_URL);
        verify(clusterUpgradeImageFilter).filter(accountId, imageCatalog, imageFilterParams);
        verify(upgradeOptionsResponseFactory).createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName());
    }

    @Test
    public void testCheckForUpgradesByNameShouldReturnImagesWhenThereAreAvailableImagesAndRepairNotChecked()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), StackType.WORKLOAD);
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        ImageCatalog imageCatalogDomain = createImageCatalogDomain();
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        UpgradeV4Response response = new UpgradeV4Response();

        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenReturn(imageCatalogDomain);
        when(imageCatalogProvider.getImageCatalogV3(CATALOG_URL)).thenReturn(imageCatalog);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        ImageFilterParams imageFilterParams = createImageFilterParams(stack, currentImageFromCatalog);
        when(imageFilterParamsFactory.create(currentImageFromCatalog, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, false, INTERNAL_UPGRADE_SETTINGS);

        assertEquals(response, actual);
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogProvider).getImageCatalogV3(CATALOG_URL);
        verify(clusterUpgradeImageFilter).filter(accountId, imageCatalog, imageFilterParams);
        verify(upgradeOptionsResponseFactory).createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName());
        verifyNoInteractions(clusterRepairService);
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnEmptyListWhenCurrentImageIsNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), DATALAKE_STACK_TYPE);
        when(imageService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("Image not found."));

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertNull(actual.getCurrent());
        assertNull(actual.getUpgradeCandidates());
        assertEquals("Failed to retrieve image due to Image not found.", actual.getReason());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnListWhenTheClusterIsNotAvailable() throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        ImageCatalog imageCatalogDomain = createImageCatalogDomain();
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        Stack stack = createStack(createStackStatus(Status.CREATE_FAILED), StackType.WORKLOAD);
        ImageFilterParams imageFilterParams = createImageFilterParams(stack, currentImageFromCatalog);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenReturn(imageCatalogDomain);
        when(imageCatalogProvider.getImageCatalogV3(CATALOG_URL)).thenReturn(imageCatalog);
        when(imageFilterParamsFactory.create(currentImageFromCatalog, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertNull(actual.getCurrent());
        assertEquals(1, actual.getUpgradeCandidates().size());
        assertEquals("Cannot upgrade cluster because it is in CREATE_FAILED state.", actual.getReason());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnEmptyListWhenTheClusterIsNotRepairable() throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {

        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        ImageCatalog imageCatalogDomain = createImageCatalogDomain();
        Result<Map<HostGroupName, Set<InstanceMetaData>>, RepairValidation> result = mock(Result.class);
        Image currentImageFromCatalog = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image properImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        Image otherImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(properImage, otherImage, currentImageFromCatalog));

        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), StackType.WORKLOAD);
        RepairValidation repairValidation = mock(RepairValidation.class);
        ImageFilterParams imageFilterParams = createImageFilterParams(stack, currentImageFromCatalog);

        when(clusterRepairService.repairWithDryRun(stack.getId())).thenReturn(result);
        when(clusterRepairService.repairWithDryRun(stack.getId())).thenReturn(result);
        when(result.isError()).thenReturn(true);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenReturn(imageCatalogDomain);
        when(imageCatalogProvider.getImageCatalogV3(CATALOG_URL)).thenReturn(imageCatalog);
        ImageFilterResult filteredImages = createFilteredImages(properImage);
        when(imageFilterParamsFactory.create(currentImageFromCatalog, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(currentImageFromCatalog, filteredImages, stack.getCloudPlatform(), stack.getRegion(),
                currentImage.getImageCatalogName())).thenReturn(response);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(currentImageFromCatalog);
        String validationError = "External RDS is not attached.";
        when(result.getError()).thenReturn(repairValidation);
        when(repairValidation.getValidationErrors()).thenReturn(Collections.singletonList(validationError));

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertNull(actual.getCurrent());
        assertEquals(1, actual.getUpgradeCandidates().size());
        assertEquals(validationError, actual.getReason());
    }

    @Test
    public void testFilterUpgradeOptionsUpgradeRequestEmpty() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        when(entitlementService.runtimeUpgradeEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true);

        assertEquals(1, response.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID_LAST, response.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testFilterUpgradeOptionsImageIdValid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(IMAGE_ID);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true);

        assertEquals(1, response.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID, response.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testFilterUpgradeOptionsImageIdInvalid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(ANOTHER_IMAGE_ID);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        Exception e = Assertions.assertThrows(BadRequestException.class, () -> underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true));
        Assert.assertEquals("The given image (another-image-id) is not eligible for the cluster upgrade. "
                + "Please choose an id from the following image(s): image-id-first,image-id-last", e.getMessage());
    }

    @Test
    public void testFilterUpgradeOptionsRuntimeValid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime(MATCHING_TARGET_RUNTIME);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertEquals(2, response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).collect(Collectors.toSet()).size());
    }

    @Test
    public void testFilterUpgradeOptionsRuntimeInvalid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime(ANOTHER_TARGET_RUNTIME);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        Exception e = Assertions.assertThrows(BadRequestException.class, () -> underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true));
        Assert.assertEquals("There is no image eligible for the cluster upgrade with runtime: 7.2.0. "
                + "Please choose a runtime from the following: 7.0.2", e.getMessage());
    }

    @Test
    public void testFilterUpgradeOptionsLockComponents() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setLockComponents(true);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertTrue(response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).anyMatch(id -> id.equals(IMAGE_ID_LAST)));
        assertTrue(response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).anyMatch(id -> id.equals(IMAGE_ID)));
    }

    @Test
    public void testFilterUpgradeOptionsDefaultCase() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setDryRun(true);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        underTest.filterUpgradeOptions(ACCOUNT_ID, response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertEquals(2, response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).collect(Collectors.toSet()).size());
    }

    @Test
    public void testFilterUpgradeOptionsFromCustomImageCatalog() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(createStackStatus(Status.AVAILABLE), StackType.WORKLOAD);
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImageWithoutImageCatalogUrl();
        Image image = mock(Image.class);
        ImageFilterParams imageFilterParams = mock(ImageFilterParams.class);
        Image imageAvailableForUpgrade = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        ImageFilterResult filteredImages = createFilteredImages(imageAvailableForUpgrade);
        UpgradeV4Response upgradeResponse = mock(UpgradeV4Response.class);
        CloudbreakImageCatalogV3 imageCatalog = createImageCatalog(List.of(image));
        ImageCatalog imageCatalogDomain = createImageCatalogDomain();

        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageProvider.getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog)).thenReturn(image);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME)).thenReturn(imageCatalogDomain);
        when(imageCatalogProvider.getImageCatalogV3(imageCatalogDomain.getImageCatalogUrl())).thenReturn(imageCatalog);
        when(imageFilterParamsFactory.create(image, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS)).thenReturn(imageFilterParams);
        when(clusterUpgradeImageFilter.filter(accountId, imageCatalog, imageFilterParams)).thenReturn(filteredImages);
        when(upgradeOptionsResponseFactory.createV4Response(image, filteredImages, CLOUD_PLATFORM, REGION, CATALOG_NAME)).thenReturn(upgradeResponse);
        when(upgradeResponse.getReason()).thenReturn("done");

        UpgradeV4Response actual = underTest.checkForUpgradesByName(stack, lockComponents, true, INTERNAL_UPGRADE_SETTINGS);

        assertEquals(upgradeResponse, actual);

        verify(imageService).getImage(stack.getId());
        verify(imageProvider).getCurrentImageFromCatalog(CURRENT_IMAGE_ID, imageCatalog);
        verify(imageCatalogService).getImageCatalogByName(stack.getWorkspace().getId(), CATALOG_NAME);
        verify(imageCatalogProvider).getImageCatalogV3(imageCatalogDomain.getImageCatalogUrl());
        verify(imageFilterParamsFactory).create(image, lockComponents, stack, INTERNAL_UPGRADE_SETTINGS);
        verify(clusterUpgradeImageFilter).filter(accountId, imageCatalog, imageFilterParams);
        verify(upgradeOptionsResponseFactory).createV4Response(image, filteredImages, CLOUD_PLATFORM, REGION, CATALOG_NAME);
    }

    private StackStatus createStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        return stackStatus;
    }

    private Stack createStack(StackStatus stackStatus, StackType stackType) {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        Stack stack = new Stack();
        stack.setId(2L);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setStackStatus(stackStatus);
        stack.setCluster(cluster);
        stack.setType(stackType);
        stack.setRegion(REGION);
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:account1:cluster:cluster");
        Tenant t = new Tenant();
        t.setName(accountId);
        User creator = new User();
        creator.setTenant(t);
        stack.setCreator(creator);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        return stack;
    }

    private ImageCatalog createImageCatalogDomain() {
        ImageCatalog imageCatalogDomain = new ImageCatalog();
        imageCatalogDomain.setImageCatalogUrl(CATALOG_URL);
        return imageCatalogDomain;
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, null, null, FALLBACK_CATALOG_URL, CATALOG_NAME, CURRENT_IMAGE_ID, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImageWithoutImageCatalogUrl() {
        return new com.sequenceiq.cloudbreak.cloud.model.Image(null, null, null, null, null, CATALOG_NAME, CURRENT_IMAGE_ID, null);
    }

    private CloudbreakImageCatalogV3 createImageCatalog(List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> images) {
        return new CloudbreakImageCatalogV3(new Images(null, images, null, null), new Versions(Collections.emptyList(), Collections.emptyList()));
    }

    private ImageFilterResult createFilteredImages(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return new ImageFilterResult(List.of(image), null);
    }

    private ImageComponentVersions createExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        return imageComponentVersions;
    }

    private ImageFilterParams createImageFilterParams(Stack stack, Image currentImageFromCatalog) {
        return new ImageFilterParams(currentImageFromCatalog, lockComponents, activatedParcels,
                stack.getType(), null, STACK_ID, INTERNAL_UPGRADE_SETTINGS, CLOUD_PLATFORM);
    }
}
