package com.sequenceiq.freeipa.service.image;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.ImageWrapper;

@Service
public class CoreImageProvider implements ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreImageProvider.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    @Inject
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor messageExtractor;

    @Inject
    private FreeIpaImageFilter freeIpaImageFilter;

    @Override
    public Optional<ImageWrapper> getImage(FreeIpaImageFilterSettings imageFilterSettings) {
        try {
            List<Image> candidateImages = getImagesInCatalogForPlatform(imageFilterSettings.catalog(), imageFilterSettings.platform());
            List<Image> images = freeIpaImageFilter.filterImages(candidateImages, imageFilterSettings);
            Optional<Image> image = freeIpaImageFilter.findMostRecentImage(images);

            return image.map(i -> ImageWrapper.ofCoreImage(i, imageFilterSettings.catalog()));
        } catch (Exception ex) {
            LOGGER.warn("Image lookup failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<ImageWrapper> getImages(FreeIpaImageFilterSettings imageFilterSettings) {
        try {
            List<Image> images = getImagesInCatalogForPlatform(imageFilterSettings.catalog(), imageFilterSettings.platform());
            return images.stream()
                    .map(i -> ImageWrapper.ofCoreImage(i, imageFilterSettings.catalog()))
                    .collect(Collectors.toList());
        } catch (WebApplicationException e) {
            String errorMessage = messageExtractor.getErrorMessage(e);
            LOGGER.warn("Fetching images failed with: {}", errorMessage, e);
            return List.of();
        } catch (Exception e) {
            LOGGER.warn("Fetching images failed", e);
            return List.of();
        }
    }

    private List<Image> getImagesInCatalogForPlatform(String catalog, String platform) throws Exception {
        ImagesV4Response imagesV4Response = imageCatalogV4Endpoint.getImagesByName(WORKSPACE_ID_DEFAULT, catalog, null,
                platform, null,
                null, false, false, null);
        LOGGER.trace("Images received: {}", imagesV4Response);
        return Optional.ofNullable(imagesV4Response.getFreeipaImages()).
                orElseGet(List::of).stream()
                .map(this::convert)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Image> convert(ImageV4Response response) {
        return Optional.ofNullable(response)
                .map(r -> new Image(r.getCreated(), r.getDate(), r.getDescription(), r.getOs(), r.getUuid(), r.getImageSetsByProvider(), r.getOsType(),
                        r.getPackageVersions(), r.isAdvertised(), r.getArchitecture(), new HashMap<>()));
    }
}
