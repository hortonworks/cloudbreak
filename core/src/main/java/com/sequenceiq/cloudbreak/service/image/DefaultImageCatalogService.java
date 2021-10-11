package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.common.api.type.ImageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;

@Component
public class DefaultImageCatalogService {

    public static final String FREEIPA_DEFAULT_CATALOG_NAME = "freeipa-default";

    public static final String CDP_DEFAULT_CATALOG_NAME = "cdp-default";

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${cb.freeipa.image.catalog.url}")
    private String defaultFreeIpaCatalogUrl;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public StatedImage getImageFromDefaultCatalog(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage;
        try {
            statedImage = imageCatalogService.getImage(defaultFreeIpaCatalogUrl, FREEIPA_DEFAULT_CATALOG_NAME, imageId);
        } catch (CloudbreakImageNotFoundException ex) {
            statedImage = imageCatalogService.getImage(imageId);
        }
        return statedImage;
    }

    public StatedImage getImageFromDefaultCatalog(String type, String provider)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        ImageType imageType = ImageType.valueOf(type);
        StatedImage statedImage;
        switch (imageType) {
            case FREEIPA:
                List<Image> images = imageCatalogProvider.getImageCatalogV3(defaultFreeIpaCatalogUrl).getImages().getFreeIpaImages();
                Optional<Image> image = images.stream().filter(i -> i.getImageSetsByProvider()
                        .keySet().stream().anyMatch(key -> key.equalsIgnoreCase(provider))).max(getImageComparing(images));
                statedImage = statedImage(image.orElseThrow(() ->
                                new CloudbreakImageNotFoundException(String.format("Could not find any image with provider: '%s' in catalog: '%s'", provider,
                                        FREEIPA_DEFAULT_CATALOG_NAME))),
                        defaultFreeIpaCatalogUrl, FREEIPA_DEFAULT_CATALOG_NAME);
                break;
            case DATAHUB:
            case DATALAKE:
            case RUNTIME:
                throw new BadRequestException(String.format("Runtime is required in case of '%s' image type", imageType));
            default:
                throw new BadRequestException(String.format("Type '%s' is not supported.", type));
        }
        return statedImage;
    }

    public StatedImage getImageFromDefaultCatalog(String type, String provider, String runtime)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        ImageType imageType = ImageType.valueOf(type);
        StatedImage statedImage;
        switch (imageType) {
            case FREEIPA:
                throw new BadRequestException(String.format("Runtime is not supported in case of '%s' image type", imageType));
            case DATAHUB:
            case DATALAKE:
            case RUNTIME:
                ImageCatalog imageCatalog = getCloudbreakDefaultImageCatalog();
                ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of(provider), null, false, null, runtime);
                statedImage = imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilter, i -> true);
                break;
            default:
                throw new BadRequestException(String.format("Image type '%s' is not supported.", type));
        }
        return statedImage;
    }

    private Comparator<Image> getImageComparing(List<Image> images) {
        return images.stream().map(Image::getCreated).anyMatch(Objects::isNull) ? Comparator.comparing(Image::getDate)
                : Comparator.comparing(Image::getCreated);
    }

    private ImageCatalog getCloudbreakDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CDP_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        return imageCatalog;
    }
}
