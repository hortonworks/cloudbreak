package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.common.api.type.ImageType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class DefaultImageCatalogService {

    public static final String FREEIPA_DEFAULT_CATALOG_NAME = "freeipa-default";

    public static final String CDP_DEFAULT_CATALOG_NAME = "cdp-default";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultImageCatalogService.class);

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${cb.freeipa.image.catalog.url}")
    private String defaultFreeIpaCatalogUrl;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public StatedImage getImageFromDefaultCatalog(Long workspaceId, String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage;
        try {
            statedImage = imageCatalogService.getImage(workspaceId, defaultFreeIpaCatalogUrl, FREEIPA_DEFAULT_CATALOG_NAME, imageId);
        } catch (CloudbreakImageNotFoundException ex) {
            statedImage = imageCatalogService.getImage(workspaceId, imageId);
        }
        return statedImage;
    }

    public StatedImage getImageFromDefaultCatalog(String type, ImageCatalogPlatform provider)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        ImageType imageType = ImageType.valueOf(type);
        StatedImage statedImage;
        switch (imageType) {
            case FREEIPA:
                List<Image> images = imageCatalogProvider.getImageCatalogV3(defaultFreeIpaCatalogUrl).getImages().getFreeIpaImages();
                Optional<Image> image = images.stream().filter(i -> i.getImageSetsByProvider()
                        .keySet().stream().anyMatch(key -> key.equalsIgnoreCase(provider.nameToLowerCase()))).max(getImageComparing(images));
                statedImage = statedImage(image.orElseThrow(() ->
                                new CloudbreakImageNotFoundException(String.format("Could not find any image with provider: '%s' in catalog: '%s'", provider,
                                        FREEIPA_DEFAULT_CATALOG_NAME))),
                        defaultFreeIpaCatalogUrl, FREEIPA_DEFAULT_CATALOG_NAME);
                break;
            case RUNTIME:
                throw new BadRequestException(String.format("Runtime is required in case of '%s' image type", imageType));
            default:
                throw new BadRequestException(String.format("Type '%s' is not supported.", type));
        }
        return statedImage;
    }

    public StatedImage getImageFromDefaultCatalog(String type, ImageCatalogPlatform provider, String runtime, Architecture architecture)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        LOGGER.info("Get default image for type: {}, provider: {}, runtime: {}, architecture: {}", type, provider, runtime, architecture);
        ImageType imageType = ImageType.valueOf(type);
        StatedImage statedImage;
        switch (imageType) {
            case FREEIPA:
                throw new BadRequestException(String.format("Runtime is not supported in case of '%s' image type", imageType));
            case RUNTIME:
                ImageFilter imageFilter = ImageFilter.builder()
                        .withArchitecture(architecture)
                        .withImageCatalog(getCloudbreakDefaultImageCatalog())
                        .withPlatforms(Set.of(provider))
                        .withBaseImageEnabled(false)
                        .withClusterVersion(runtime)
                        .build();
                statedImage = imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilter);
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
