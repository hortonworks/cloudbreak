package com.sequenceiq.cloudbreak.service.image;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.VmImage;

@Component
public class CustomImageCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomImageCatalogService.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public Set<ImageCatalog> getImageCatalogs(Long workspaceId) {
        LOGGER.debug(String.format("List custom image catalogs in workspace '%d'", workspaceId));
        return imageCatalogService.findAllByWorkspaceId(workspaceId, true);
    }

    public ImageCatalog getImageCatalog(Long workspaceId, String imageCatalogName) {
        LOGGER.debug(String.format("Get custom image catalog '%s' from workspace '%d'", imageCatalogName, workspaceId));
        ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, imageCatalogName);

        if (Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl())) {
            return imageCatalog;
        } else {
            throw new BadRequestException(String.format("'%s' is not a custom image catalog.", imageCatalog.getName()));
        }
    }

    public ImageCatalog create(ImageCatalog imageCatalog, Long workspaceId, String accountId, String creator) {
        LOGGER.debug(String.format("Create new custom image catalog '%s' in workspace '%d'", imageCatalog.getName(), workspaceId));
        Optional<ImageCatalog> optionalImageCatalog = imageCatalogService.repository().findByNameAndWorkspaceId(imageCatalog.getName(), workspaceId);
        if (optionalImageCatalog.isEmpty()) {
            return imageCatalogService.createForLoggedInUser(imageCatalog, workspaceId, accountId, creator);
        } else {
            LOGGER.error(String.format("Image catalog '%s' is already available in workspace '%d'", imageCatalog.getName(), workspaceId));
            throw new BadRequestException(String.format("ImageCatalog or CustomImageCatalog is already available with name '%s'.", imageCatalog.getName()));
        }
    }

    public ImageCatalog delete(Long workspaceId, String imageCatalogName) {
        LOGGER.debug(String.format("Delete custom image catalog '%s' in workspace '%d'", imageCatalogName, workspaceId));
        ImageCatalog imageCatalog = imageCatalogService.get(workspaceId, imageCatalogName);
        if (Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl())) {
            return imageCatalogService.delete(workspaceId, imageCatalogName);
        } else {
            throw new BadRequestException(String.format("'%s' is not a custom image catalog.", imageCatalog.getName()));
        }
    }

    public CustomImage getCustomImage(Long workspaceId, String imageCatalogName, String imageId) {
        LOGGER.debug(String.format("Get custom image '%s' from catalog '%s' in workspace '%d'", imageId, imageCatalogName, workspaceId));
        ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);
        return getCustomImageFromCatalog(imageCatalog, imageId);
    }

    public Image getSourceImage(CustomImage image) {
        String imageId = image.getCustomizedImageId();
        LOGGER.debug(String.format("Get source image '%s' from default catalog", imageId));
        try {
            StatedImage sourceStatedImage = imageCatalogService.getSourceImageByImageType(image);
            return sourceStatedImage.getImage();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.error(String.format("Failed to get source image: %s", e.getMessage()));
            throw new NotFoundException(String.format("Could not find source image with id: '%s'", imageId));
        }
    }

    public CustomImage createCustomImage(Long workspaceId, String accountId, String creator, String imageCatalogName, CustomImage customImage) {
        String imageName = UUID.randomUUID().toString();
        LOGGER.debug(String.format("Create custom image '%s' in catalog '%s' in workspace '%d'", imageName, imageCatalogName, workspaceId));

        try {
            return transactionService.required(() -> {
                ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);
                customImage.setName(imageName);
                customImage.setCreator(creator);
                customImage.setResourceCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.IMAGE_CATALOG, imageName, accountId));
                customImage.setImageCatalog(imageCatalog);
                customImage.getVmImage().stream().forEach(vmImage -> {
                    vmImage.setCustomImage(customImage);
                    vmImage.setCreator(creator);
                });

                validateSourceImage(customImage);

                imageCatalog.getCustomImages().add(customImage);
                ImageCatalog result = imageCatalogService.pureSave(imageCatalog);

                return getCustomImageFromCatalog(result, imageName);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error(String.format("Custom image creation failed: %s", e.getMessage()));
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public CustomImage deleteCustomImage(Long workspaceId, String imageCatalogName, String imageId) {
        LOGGER.debug(String.format("Delete custom image '%s' from catalog '%s' in workspace '%d'", imageId, imageCatalogName, workspaceId));

        try {
            return transactionService.required(() -> {
                ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);

                CustomImage customImage = getCustomImageFromCatalog(imageCatalog, imageId);
                imageCatalog.getCustomImages().remove(customImage);

                imageCatalogService.pureSave(imageCatalog);
                return customImage;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error(String.format("Custom image '%s' delete failed from custom image catalog '%s' in workspace '%d': %s",
                    imageId, imageCatalogName, workspaceId, e.getMessage()));
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public CustomImage updateCustomImage(Long workspaceId, String creator, String imageCatalogName, CustomImage customImage) {
        LOGGER.debug(String.format("Update custom image '%s' in catalog '%s' in workspace '%d'",
                customImage.getName(), imageCatalogName, workspaceId));

        try {
            return transactionService.required(() -> {
                ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);

                CustomImage savedCustomImage = getCustomImageFromCatalog(imageCatalog, customImage.getName());

                if (customImage.getCustomizedImageId() != null) {
                    savedCustomImage.setCustomizedImageId(customImage.getCustomizedImageId());
                }
                if (customImage.getImageType() != null) {
                    savedCustomImage.setImageType(customImage.getImageType());
                }
                if (customImage.getBaseParcelUrl() != null) {
                    savedCustomImage.setBaseParcelUrl(customImage.getBaseParcelUrl());
                }
                if (customImage.getVmImage() != null) {
                    Set<VmImage> vmImagesToSave = new HashSet<>(customImage.getVmImage());
                    for (VmImage savedVmImage : savedCustomImage.getVmImage()) {
                        Optional<VmImage> vmImage = vmImagesToSave.stream().filter(vm -> vm.getRegion().equals(savedVmImage.getRegion())).findFirst();
                        if (vmImage.isPresent()) {
                            savedVmImage.setRegion(vmImage.get().getRegion());
                            savedVmImage.setImageReference(vmImage.get().getImageReference());
                            vmImagesToSave.remove(vmImage.get());
                        } else {
                            savedCustomImage.getVmImage().remove(savedVmImage);
                        }
                    }
                    for (VmImage vmImage : vmImagesToSave) {
                        vmImage.setCreator(creator);
                        vmImage.setCustomImage(savedCustomImage);
                        savedCustomImage.getVmImage().add(vmImage);
                    }
                }

                validateSourceImage(savedCustomImage);

                ImageCatalog savedImageCatalog = imageCatalogService.pureSave(imageCatalog);

                return getCustomImageFromCatalog(savedImageCatalog, customImage.getName());
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error(String.format("Custom image '%s' update failed in custom image catalog '%s' in workspace '%d': %s",
                    customImage.getName(), imageCatalogName, workspaceId, e.getMessage()));
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private CustomImage getCustomImageFromCatalog(ImageCatalog imageCatalog, String imageId) {
        return imageCatalog.getCustomImages().stream().filter(ci -> ci.getName().equalsIgnoreCase(imageId)).findFirst().orElseThrow(
                () -> new NotFoundException(String.format("Could not find any image with id: '%s' in catalog: '%s'", imageId, imageCatalog.getName())));
    }

    private void validateSourceImage(CustomImage customImage) {
        try {
            imageCatalogService.getSourceImageByImageType(customImage);
        } catch (Exception ex) {
            LOGGER.error(String.format("Could not find '%s' image in default image catalog with id '%s'. %s",
                    customImage.getImageType(), customImage.getCustomizedImageId(), ex.getMessage()));
            throw new NotFoundException(String.format("Could not find '%s' image in default image catalog with id '%s'",
                    customImage.getImageType(), customImage.getCustomizedImageId()));
        }
    }
}
