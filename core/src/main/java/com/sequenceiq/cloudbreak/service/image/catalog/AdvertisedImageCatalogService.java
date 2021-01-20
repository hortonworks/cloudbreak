package com.sequenceiq.cloudbreak.service.image.catalog;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageFilter;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class AdvertisedImageCatalogService implements ImageCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisedImageCatalogService.class);

    @Inject
    private AdvertisedImageProvider advertisedImageProvider;

    @Override
    public StatedImages getImages(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilter imageFilter) {
        return advertisedImageProvider.getImages(imageCatalogV3, imageFilter);
    }

    @Override
    public void validate(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        validateVersion(imageCatalogV3);
        validateAdvertisedCdhImageExistance(imageCatalogV3);
    }

    @Override
    public ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3) {
        List<Image> images = imageCatalogV3.getImages().getCdhImages().stream().filter(Image::isAdvertised).collect(toList());
        LOGGER.debug("{} images found by the advertised flag", images.size());
        String message = String.format("%d images found by the advertised flag", images.size());

        return new ImageFilterResult(new Images(null, images, null), message);
    }

    private void validateVersion(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        if (imageCatalogV3.getVersions() != null) {
            throw new CloudbreakImageCatalogException("Versions should be null.");
        }
    }

    private void validateAdvertisedCdhImageExistance(CloudbreakImageCatalogV3 imageCatalogV3) throws CloudbreakImageCatalogException {
        if (imageCatalogV3.getImages().getCdhImages().stream().noneMatch(i -> i.isAdvertised())) {
            throw new CloudbreakImageCatalogException("There should be at least one advertised cdh image.");
        }
    }
}
