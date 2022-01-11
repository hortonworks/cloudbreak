package com.sequenceiq.cloudbreak.service.image;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;

@Service
public class GenerateImageCatalogService {

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    public CloudbreakImageCatalogV3 generateImageCatalogForStack(Stack stack) {
        try {
            Image image = stackImageService.getCurrentImage(stack);
            if (!Strings.isNullOrEmpty(image.getImageCatalogUrl())) {
                StatedImage statedImage = imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId());
                if (Strings.isNullOrEmpty(statedImage.getImage().getSourceImageId())) {
                    Images images = new Images(null, List.of(copyCatalogImageAndSetAdvertisedFlag(statedImage.getImage())), null, null);
                    return new CloudbreakImageCatalogV3(images, null);
                } else {
                    throw new CloudbreakServiceException(
                            String.format("Image '%s' of stack '%s' is a customized image. Image catalog generation is not allowed!",
                                    image.getImageCatalogName(), stack.getName()));
                }
            } else {
                throw new CloudbreakServiceException(
                        String.format("Image catalog '%s' of stack '%s' is not a json based image catalog. Image catalog generation is not allowed!",
                                image.getImageCatalogName(), stack.getName()));
            }
        } catch (CloudbreakImageNotFoundException ex) {
            throw new NotFoundException(ex.getMessage(), ex);
        } catch (CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image copyCatalogImageAndSetAdvertisedFlag(
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image source) {
        return new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(
                source.getDate(),
                source.getCreated(),
                source.getPublished(),
                source.getDescription(),
                source.getOs(),
                source.getUuid(),
                source.getVersion(),
                source.getRepo(),
                source.getImageSetsByProvider(),
                source.getStackDetails(),
                source.getOsType(),
                source.getPackageVersions(),
                source.getPreWarmParcels(),
                source.getPreWarmCsd(),
                source.getCmBuildNumber(),
                true,
                source.getBaseParcelUrl(),
                source.getSourceImageId()
        );
    }
}
