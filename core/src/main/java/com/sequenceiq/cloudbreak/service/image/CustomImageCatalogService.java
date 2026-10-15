package com.sequenceiq.cloudbreak.service.image;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
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
        LOGGER.debug("List custom image catalogs in workspace '{}'", workspaceId);
        return imageCatalogService.findAllByWorkspaceId(workspaceId, true);
    }

    public ImageCatalog getImageCatalog(Long workspaceId, String imageCatalogName) {
        LOGGER.debug("Get custom image catalog '{}' from workspace '{}'", imageCatalogName, workspaceId);
        ImageCatalog imageCatalog = imageCatalogService.getImageCatalogByName(workspaceId, imageCatalogName);

        if (Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl())) {
            return imageCatalog;
        } else {
            throw new BadRequestException(String.format("'%s' is not a custom image catalog.", imageCatalog.getName()));
        }
    }

    public ImageCatalog create(ImageCatalog imageCatalog, Long workspaceId, String accountId, String creator) {
        LOGGER.debug("Create new custom image catalog '{}' in workspace '{}'", imageCatalog.getName(), workspaceId);
        Optional<ImageCatalog> optionalImageCatalog = imageCatalogService.repository().findByNameAndWorkspaceId(imageCatalog.getName(), workspaceId);
        if (optionalImageCatalog.isEmpty()) {
            return imageCatalogService.createForLoggedInUser(imageCatalog, workspaceId, accountId, creator);
        } else {
            LOGGER.error("Image catalog '{}' is already available in workspace '{}'", imageCatalog.getName(), workspaceId);
            throw new BadRequestException(String.format("ImageCatalog or CustomImageCatalog is already available with name '%s'.", imageCatalog.getName()));
        }
    }

    public ImageCatalog delete(Long workspaceId, String imageCatalogName) {
        LOGGER.debug("Delete custom image catalog '{}' in workspace '{}'", imageCatalogName, workspaceId);
        ImageCatalog imageCatalog = imageCatalogService.getImageCatalogByName(workspaceId, imageCatalogName);
        if (Strings.isNullOrEmpty(imageCatalog.getImageCatalogUrl())) {
            return imageCatalogService.delete(workspaceId, imageCatalogName);
        } else {
            throw new BadRequestException(String.format("'%s' is not a custom image catalog.", imageCatalog.getName()));
        }
    }

    public CustomImage getCustomImage(Long workspaceId, String imageCatalogName, String imageId) {
        LOGGER.debug("Get custom image '{}' from catalog '{}' in workspace '{}'", imageId, imageCatalogName, workspaceId);
        ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);
        return getCustomImageFromCatalog(imageCatalog, imageId);
    }

    public Image getSourceImage(CustomImage image) {
        String imageId = image.getCustomizedImageId();
        LOGGER.debug("Get source image '{}' from default catalog", imageId);
        try {
            StatedImage sourceStatedImage = imageCatalogService.getSourceImageByImageType(image);
            return sourceStatedImage.getImage();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.error("Failed to get source image: {}", e.getMessage());
            throw new NotFoundException(String.format("Could not find source image with id: '%s'", imageId));
        }
    }

    public CustomImage createOrUpdateCustomImage(Long workspaceId, String accountId, String imageCatalogName, CustomImage customImage) {
        try {
            return transactionService.required(() -> {
                ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);
                customImage.setImageCatalog(imageCatalog);
                validateSourceImage(customImage);

                List<CustomImage> matches = findMatchingCustomImages(imageCatalog, customImage);
                CustomImage upserted = matches.isEmpty()
                        ? addNewCustomImage(imageCatalog, customImage, accountId, imageCatalogName, workspaceId)
                        : replaceMatchingCustomImages(imageCatalog, matches, customImage, imageCatalogName);

                ImageCatalog savedCatalog = imageCatalogService.pureSave(imageCatalog);
                return getCustomImageFromCatalog(savedCatalog, upserted.getName());
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Custom image creation failed: {}", e.getMessage());
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private List<CustomImage> findMatchingCustomImages(ImageCatalog imageCatalog, CustomImage customImage) {
        return imageCatalog.getCustomImages().stream()
                .filter(existing -> existing.getCustomizedImageId().equalsIgnoreCase(customImage.getCustomizedImageId())
                        && existing.getImageType() == customImage.getImageType())
                .sorted(Comparator.comparing(CustomImage::getCreated).reversed())
                .toList();
    }

    private CustomImage addNewCustomImage(ImageCatalog imageCatalog, CustomImage customImage, String accountId, String imageCatalogName,
            Long workspaceId) {
        String imageName = UUID.randomUUID().toString();
        LOGGER.debug("Creating custom image '{}' in catalog '{}' in workspace '{}'", imageName, imageCatalogName, workspaceId);
        customImage.setName(imageName);
        customImage.setResourceCrn(regionAwareCrnGenerator.generateCrnString(CrnResourceDescriptor.IMAGE_CATALOG, imageName, accountId));
        customImage.getVmImage().forEach(vmImage -> vmImage.setCustomImage(customImage));
        imageCatalog.getCustomImages().add(customImage);
        return customImage;
    }

    private CustomImage replaceMatchingCustomImages(ImageCatalog imageCatalog, List<CustomImage> matches, CustomImage customImage,
            String imageCatalogName) {
        CustomImage mostRecent = matches.get(0);
        LOGGER.info("Found {} custom image(s) for source '{}' (type {}) in catalog '{}'; removing duplicates and updating '{}'.", matches.size(),
                customImage.getCustomizedImageId(), customImage.getImageType(), imageCatalogName, mostRecent.getName());
        matches.stream().skip(1).forEach(imageCatalog.getCustomImages()::remove);
        if (customImage.getBaseParcelUrl() != null) {
            mostRecent.setBaseParcelUrl(customImage.getBaseParcelUrl());
        }
        mergeVmImagesByRegion(mostRecent, customImage.getVmImage());
        return mostRecent;
    }

    public CustomImage deleteCustomImage(Long workspaceId, String imageCatalogName, String imageId) {
        LOGGER.debug("Delete custom image '{}' from catalog '{}' in workspace '{}'", imageId, imageCatalogName, workspaceId);

        try {
            return transactionService.required(() -> {
                ImageCatalog imageCatalog = getImageCatalog(workspaceId, imageCatalogName);

                CustomImage customImage = getCustomImageFromCatalog(imageCatalog, imageId);
                imageCatalog.getCustomImages().remove(customImage);

                imageCatalogService.pureSave(imageCatalog);
                return customImage;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Custom image '{}' delete failed from custom image catalog '{}' in workspace '{}': {}", imageId, imageCatalogName, workspaceId,
                    e.getMessage());
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public CustomImage updateCustomImage(Long workspaceId, String imageCatalogName, CustomImage customImage) {
        LOGGER.debug("Update custom image '{}' in catalog '{}' in workspace '{}'", customImage.getName(), imageCatalogName, workspaceId);

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
                    mergeVmImagesByRegion(savedCustomImage, customImage.getVmImage());
                }

                validateSourceImage(savedCustomImage);

                ImageCatalog savedImageCatalog = imageCatalogService.pureSave(imageCatalog);

                return getCustomImageFromCatalog(savedImageCatalog, customImage.getName());
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Custom image '{}' update failed in custom image catalog '{}' in workspace '{}': {}", customImage.getName(), imageCatalogName,
                    workspaceId, e.getMessage());
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private CustomImage getCustomImageFromCatalog(ImageCatalog imageCatalog, String imageId) {
        return imageCatalog.getCustomImages().stream().filter(ci -> ci.getName().equalsIgnoreCase(imageId)).findFirst().orElseThrow(
                () -> new NotFoundException(String.format("Could not find any image with id: '%s' in catalog: '%s'", imageId, imageCatalog.getName())));
    }

    private void mergeVmImagesByRegion(CustomImage target, Set<VmImage> incomingVmImages) {
        updateMatchingRegionsInPlace(target, incomingVmImages);
        removeRegionsMissingFrom(target, incomingVmImages);
        addNewRegions(target, incomingVmImages);
    }

    private void updateMatchingRegionsInPlace(CustomImage target, Set<VmImage> incomingVmImages) {
        for (VmImage existing : target.getVmImage()) {
            findByRegion(incomingVmImages, existing.getRegion())
                    .ifPresent(incoming -> existing.setImageReference(incoming.getImageReference()));
        }
    }

    private void removeRegionsMissingFrom(CustomImage target, Set<VmImage> incomingVmImages) {
        target.getVmImage().removeIf(existing -> findByRegion(incomingVmImages, existing.getRegion()).isEmpty());
    }

    private void addNewRegions(CustomImage target, Set<VmImage> incomingVmImages) {
        for (VmImage incoming : incomingVmImages) {
            if (findByRegion(target.getVmImage(), incoming.getRegion()).isEmpty()) {
                incoming.setCustomImage(target);
                target.getVmImage().add(incoming);
            }
        }
    }

    private Optional<VmImage> findByRegion(Set<VmImage> vmImages, String region) {
        return vmImages.stream().filter(vmImage -> region.equals(vmImage.getRegion())).findFirst();
    }

    private void validateSourceImage(CustomImage customImage) {
        try {
            imageCatalogService.getSourceImageByImageType(customImage);
        } catch (Exception ex) {
            LOGGER.error("Could not find '{}' image in default image catalog with id '{}'. {}", customImage.getImageType(), customImage.getCustomizedImageId(),
                    ex.getMessage());
            throw new NotFoundException(String.format("Could not find '%s' image in default image catalog with id '%s'",
                    customImage.getImageType(), customImage.getCustomizedImageId()));
        }
    }
}
