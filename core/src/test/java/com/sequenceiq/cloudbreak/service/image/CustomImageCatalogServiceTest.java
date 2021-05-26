package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.VmImage;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.type.ImageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomImageCatalogServiceTest {

    private static final Long WORKSPACE_ID = 123L;

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String ACCOUNT_ID = "account id";

    private static final String CREATOR = "creator";

    private static final String IMAGE_NAME = "image name";

    private static final String CUSTOMIZED_IMAGE_ID = "customized image id";

    private static final String BASE_PARCEL_URL = "base parcel url";

    private static final String IMAGE_REFERENCE = "image reference";

    private static final String REGION = "region";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private WorkspaceResourceRepository<ImageCatalog, Long> repository;

    @InjectMocks
    private CustomImageCatalogService victim;

    @Test
    public void testGetImageCatalogs() {
        Set<ImageCatalog> expected = new HashSet<>();

        when(imageCatalogService.findAllByWorkspaceId(WORKSPACE_ID, true)).thenReturn(expected);

        Set<ImageCatalog> actual = victim.getImageCatalogs(WORKSPACE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageCatalog() {
        ImageCatalog expected = new ImageCatalog();

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(expected);

        ImageCatalog actual = victim.getImageCatalog(WORKSPACE_ID, IMAGE_CATALOG_NAME);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetImageCatalogShouldFailInCaseOfNonCustomImageCatalog() {
        ImageCatalog expected = new ImageCatalog();
        expected.setImageCatalogUrl(IMAGE_CATALOG_URL);

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(expected);

        assertThrows(BadRequestException.class, () -> victim.getImageCatalog(WORKSPACE_ID, IMAGE_CATALOG_NAME));
    }

    @Test
    public void testCreate() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(IMAGE_CATALOG_NAME);
        ImageCatalog expected = new ImageCatalog();

        when(imageCatalogService.repository()).thenReturn(repository);
        when(repository.findByNameAndWorkspaceId(IMAGE_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());
        when(imageCatalogService.createForLoggedInUser(imageCatalog, WORKSPACE_ID, ACCOUNT_ID, CREATOR)).thenReturn(expected);

        ImageCatalog actual = victim.create(imageCatalog, WORKSPACE_ID, ACCOUNT_ID, CREATOR);

        assertEquals(expected, actual);
    }

    @Test
    public void testCreateFailsOnAlreadyExistingCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(IMAGE_CATALOG_NAME);
        ImageCatalog savedImageCatalog = new ImageCatalog();

        when(imageCatalogService.repository()).thenReturn(repository);
        when(repository.findByNameAndWorkspaceId(IMAGE_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(savedImageCatalog));

        assertThrows(BadRequestException.class, () -> victim.create(imageCatalog, WORKSPACE_ID, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testDelete() {
        ImageCatalog expected = new ImageCatalog();

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(expected);
        when(imageCatalogService.delete(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(expected);

        ImageCatalog actual = victim.delete(WORKSPACE_ID, IMAGE_CATALOG_NAME);

        assertEquals(expected, actual);
    }

    @Test
    public void testDeleteShouldFailInCaseOfNonCustomImageCatalog() {
        ImageCatalog expected = new ImageCatalog();
        expected.setImageCatalogUrl(IMAGE_CATALOG_URL);

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(expected);

        assertThrows(BadRequestException.class, () -> victim.delete(WORKSPACE_ID, IMAGE_CATALOG_NAME));
    }

    @Test
    public void testGetCustomImage() {
        CustomImage expected = new CustomImage();
        expected.setName(IMAGE_NAME);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCustomImages(Collections.singleton(expected));

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        CustomImage actual = victim.getCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_NAME);

        assertEquals(expected, actual);
    }

    @Test
    public void testGetCustomImageShouldFailInCaseOfNonCustomImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        assertThrows(BadRequestException.class, () -> victim.getCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_NAME));
    }

    @Test
    public void testGetCustomImageShouldFailOnImageNotFound() {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImage expected = new CustomImage();
        expected.setName(IMAGE_NAME);

        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        assertThrows(NotFoundException.class, () -> victim.getCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_NAME));
    }

    @Test
    public void testGetSourceImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CustomImage customImage = new CustomImage();
        customImage.setCustomizedImageId(CUSTOMIZED_IMAGE_ID);
        Image expected = createTestImage();
        StatedImage statedImage = StatedImage.statedImage(expected, null, IMAGE_CATALOG_NAME);

        when(imageCatalogService.getSourceImageByImageType(customImage)).thenReturn(statedImage);

        Image actual = victim.getSourceImage(customImage);

        assertEquals(expected, actual);
    }

    @Test
    public void testCreateCustomImage() throws TransactionService.TransactionExecutionException {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImage expected = aCustomImage();

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.createCustomImage(WORKSPACE_ID, ACCOUNT_ID, CREATOR, IMAGE_CATALOG_NAME, expected);
        VmImage actualVmImage = actual.getVmImage().stream().findFirst().get();

        assertNotEquals(IMAGE_NAME, actual.getName());
        assertEquals(CUSTOMIZED_IMAGE_ID, actual.getCustomizedImageId());
        assertEquals(BASE_PARCEL_URL, actual.getBaseParcelUrl());
        assertEquals(ImageType.DATALAKE, actual.getImageType());
        assertEquals(CREATOR, actual.getCreator());
        assertNotNull(actual.getResourceCrn());
        assertEquals(imageCatalog, actual.getImageCatalog());
        assertEquals(1, actual.getVmImage().size());
        assertEquals(CREATOR, actualVmImage.getCreator());
        assertEquals(REGION, actualVmImage.getRegion());
        assertEquals(IMAGE_REFERENCE, actualVmImage.getImageReference());
        assertEquals(actual, actualVmImage.getCustomImage());
    }

    @Test
    public void testCreateCustomImageShouldFailInCaseOfNonCustomImageCatalog() throws TransactionService.TransactionExecutionException,
            CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImage customImage = aCustomImage();

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.getSourceImageByImageType(customImage)).thenThrow(new CloudbreakImageCatalogException(""));



        assertThrows(NotFoundException.class, () -> victim.createCustomImage(WORKSPACE_ID, ACCOUNT_ID, CREATOR, IMAGE_CATALOG_NAME, customImage));
    }

    @Test
    public void testCreateCustomImageShouldFailInCaseOfMissingSourceImage() throws TransactionService.TransactionExecutionException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        assertThrows(BadRequestException.class, () -> victim.createCustomImage(WORKSPACE_ID, ACCOUNT_ID, CREATOR, IMAGE_CATALOG_NAME, null));
    }

    @Test
    public void testDeleteCustomImage() throws TransactionService.TransactionExecutionException {
        CustomImage expected = aCustomImage();
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(expected);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.deleteCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_NAME);

        assertTrue(imageCatalog.getCustomImages().isEmpty());
        assertEquals(expected, actual);
    }

    @Test
    public void testDeleteCustomImageShouldFailInCaseOfNonCustomImageCatalog() throws TransactionService.TransactionExecutionException {
        CustomImage expected = aCustomImage();
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        assertThrows(BadRequestException.class, () -> victim.deleteCustomImage(WORKSPACE_ID, IMAGE_CATALOG_NAME, IMAGE_NAME));
    }

    @Test
    public void testUpdateCustomImageWithNewVmImage() throws TransactionService.TransactionExecutionException {
        CustomImage updatedCustomImage = aCustomImage();
        CustomImage savedCustomImage = new CustomImage();
        savedCustomImage.setName(IMAGE_NAME);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(savedCustomImage);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, updatedCustomImage);
        assertEquals(1, imageCatalog.getCustomImages().size());
        assertEquals(CUSTOMIZED_IMAGE_ID, actual.getCustomizedImageId());
        assertEquals(BASE_PARCEL_URL, actual.getBaseParcelUrl());
        assertEquals(ImageType.DATALAKE, actual.getImageType());
        assertEquals(1, actual.getVmImage().size());

        VmImage actualVmImage = actual.getVmImage().stream().findFirst().get();
        assertEquals(CREATOR, actualVmImage.getCreator());
        assertEquals(REGION, actualVmImage.getRegion());
        assertEquals(IMAGE_REFERENCE, actualVmImage.getImageReference());
    }

    @Test
    public void testUpdateCustomImageWithUpdatedVmImage() throws TransactionService.TransactionExecutionException {
        CustomImage updatedCustomImage = aCustomImage();
        CustomImage savedCustomImage = new CustomImage();
        VmImage vmImage = new VmImage();
        vmImage.setRegion(REGION);
        savedCustomImage.getVmImage().add(vmImage);
        savedCustomImage.setName(IMAGE_NAME);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(savedCustomImage);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, updatedCustomImage);
        assertEquals(1, imageCatalog.getCustomImages().size());
        assertEquals(CUSTOMIZED_IMAGE_ID, actual.getCustomizedImageId());
        assertEquals(BASE_PARCEL_URL, actual.getBaseParcelUrl());
        assertEquals(ImageType.DATALAKE, actual.getImageType());
        assertEquals(1, actual.getVmImage().size());

        VmImage actualVmImage = actual.getVmImage().stream().findFirst().get();
        assertNull(actualVmImage.getCreator());
        assertEquals(REGION, actualVmImage.getRegion());
        assertEquals(IMAGE_REFERENCE, actualVmImage.getImageReference());
    }

    @Test
    public void testUpdateCustomImageWithDeletedVmImage() throws TransactionService.TransactionExecutionException {
        CustomImage updatedCustomImage = aCustomImage();
        updatedCustomImage.setVmImage(Collections.emptySet());
        CustomImage savedCustomImage = new CustomImage();
        VmImage vmImage = new VmImage();
        vmImage.setRegion(REGION);
        savedCustomImage.getVmImage().add(vmImage);
        savedCustomImage.setName(IMAGE_NAME);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(savedCustomImage);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, updatedCustomImage);

        assertEquals(1, imageCatalog.getCustomImages().size());
        assertEquals(CUSTOMIZED_IMAGE_ID, actual.getCustomizedImageId());
        assertEquals(BASE_PARCEL_URL, actual.getBaseParcelUrl());
        assertEquals(ImageType.DATALAKE, actual.getImageType());
        assertEquals(0, actual.getVmImage().size());
    }

    @Test
    public void testUpdateCustomImageWithNullsHasNoAffect() throws TransactionService.TransactionExecutionException {
        CustomImage updatedCustomImage = new CustomImage();
        updatedCustomImage.setVmImage(null);
        updatedCustomImage.setName(IMAGE_NAME);
        CustomImage savedCustomImage = aCustomImage();
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(savedCustomImage);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.pureSave(imageCatalog)).thenReturn(imageCatalog);

        CustomImage actual = victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, updatedCustomImage);
        assertEquals(1, imageCatalog.getCustomImages().size());
        assertEquals(CUSTOMIZED_IMAGE_ID, actual.getCustomizedImageId());
        assertEquals(BASE_PARCEL_URL, actual.getBaseParcelUrl());
        assertEquals(ImageType.DATALAKE, actual.getImageType());
        assertEquals(1, actual.getVmImage().size());

        VmImage actualVmImage = actual.getVmImage().stream().findFirst().get();
        assertEquals(REGION, actualVmImage.getRegion());
        assertEquals(IMAGE_REFERENCE, actualVmImage.getImageReference());
    }

    @Test
    public void testUpdateCustomImageShouldFailInCaseOfNonCustomImageCatalog() throws TransactionService.TransactionExecutionException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(IMAGE_CATALOG_URL);
        CustomImage customImage = new CustomImage();

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        assertThrows(BadRequestException.class, () -> victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, customImage));
    }

    @Test
    public void testUpdateCustomImageShouldFailInCaseMissingSourceImage() throws TransactionService.TransactionExecutionException,
            CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        CustomImage updatedCustomImage = new CustomImage();
        updatedCustomImage.setVmImage(null);
        updatedCustomImage.setName(IMAGE_NAME);
        updatedCustomImage.setCustomizedImageId("fakeid");
        CustomImage savedCustomImage = aCustomImage();
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.getCustomImages().add(savedCustomImage);

        doAnswer(invocation -> ((Supplier<CustomImage>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);
        when(imageCatalogService.getSourceImageByImageType(savedCustomImage)).thenThrow(new CloudbreakImageCatalogException(""));

        assertThrows(NotFoundException.class, () -> victim.updateCustomImage(WORKSPACE_ID, CREATOR, IMAGE_CATALOG_NAME, updatedCustomImage));
    }

    private CustomImage aCustomImage() {
        CustomImage customImage = new CustomImage();
        VmImage vmImage = new VmImage();
        vmImage.setImageReference(IMAGE_REFERENCE);
        vmImage.setRegion(REGION);
        customImage.setName(IMAGE_NAME);
        customImage.setCustomizedImageId(CUSTOMIZED_IMAGE_ID);
        customImage.setImageType(ImageType.DATALAKE);
        customImage.setBaseParcelUrl(BASE_PARCEL_URL);
        customImage.setVmImage(Collections.singleton(vmImage));

        return customImage;
    }

    private Image createTestImage() {
        return new Image(null, null, null, null, CUSTOMIZED_IMAGE_ID, null, null, null, null, null, null, null, null, null, true, null, null);
    }
}