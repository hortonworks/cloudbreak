package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Service
public class CmSyncImageCollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncImageCollectorService.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageService imageService;

    /**
     * Will collect all images using the stack's image catalog.
     * If the candidateImageUuids are empty, all images for the given cloud platform are returned.
     * TODO: Currently, however, if no candidateImageUuids are supplied, only the last patch for every line is returned. This needs to be fixed
     * @param userCrn The user who executes
     * @param stack The stack for which the image uuid are to be resolved
     * @param candidateImageUuids A set of image uuids that are resolved to images. If empty, all applicable images are returned
     * @return A set of found images
     */
    public Set<Image> collectImages(String userCrn, Stack stack, Set<String> candidateImageUuids) {
        try {
            String imageCatalogName = imageService.getCurrentImageCatalogName(stack.getId());
            Long workspaceId = stack.getWorkspace().getId();
            return candidateImageUuids.isEmpty()
                    ? getAllImagesFromCatalog(userCrn, stack, imageCatalogName, workspaceId)
                    : getCurrentAndSelectedImagesFromCatalog(stack, candidateImageUuids, imageCatalogName, workspaceId);
        } catch (Exception e) {
            LOGGER.warn("It is not possible to collect images for CM sync, returning empty collection: ", e);
            return Set.of();
        }
    }

    private Set<Image> getCurrentAndSelectedImagesFromCatalog(Stack stack, Set<String> candidateImageUuids, String imageCatalogName, Long workspaceId)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Set<Image> candidateImages = candidateImageUuids.stream()
                .map(uuid -> getImage(imageCatalogName, workspaceId, uuid))
                .collect(Collectors.toCollection(HashSet::new));
        candidateImages.add(imageService.getCurrentImage(stack.getId()).getImage());
        return candidateImages;
    }

    private Set<Image> getAllImagesFromCatalog(String userCrn, Stack stack, String imageCatalogName, Long workspaceId) throws CloudbreakImageCatalogException {
        List<Image> allCdhImages = imageCatalogService.getCdhImages(userCrn, workspaceId, imageCatalogName, stack.cloudPlatform());
        return new HashSet<>(allCdhImages);
    }

    private Image getImage(String imageCatalogName, Long workspaceId, String uuid) {
        try {
            return imageCatalogService.getImageByCatalogName(workspaceId, uuid, imageCatalogName).getImage();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("Error finding image by UUID: ", e);
            throw new CloudbreakServiceException(e);
        }
    }

}