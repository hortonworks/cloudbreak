package com.sequenceiq.freeipa.service.image;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;

@Service
public class CoreImageProvider implements ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreImageProvider.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    @Value("${freeipa.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor messageExtractor;

    @Override
    public Optional<ImageWrapper> getImage(ImageSettingsRequest imageSettings, String region, String platform) {
        try {
            ImageV4Response imageV4Response = imageCatalogV4Endpoint.getSingleImageByCatalogNameAndImageId(
                    WORKSPACE_ID_DEFAULT, imageSettings.getCatalog(), imageSettings.getId());

            Optional<Image> image = convert(imageV4Response);

            return image.map(i -> new ImageWrapper(i, defaultCatalogUrl, imageSettings.getCatalog()));
        } catch (Exception ex) {
            LOGGER.warn("Image lookup failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<ImageWrapper> getImages(ImageSettingsRequest imageSettings, String region, String platform) {
        try {
            ImagesV4Response imagesV4Response = imageCatalogV4Endpoint.getImagesByName(WORKSPACE_ID_DEFAULT, imageSettings.getCatalog(), null, platform, null,
                    null);
            LOGGER.debug("Images received: {}", imagesV4Response);
            return Optional.ofNullable(imagesV4Response.getFreeipaImages()).
                    orElseGet(List::of).stream()
                    .map(this::convert)
                    .flatMap(Optional::stream)
                    .map(img -> new ImageWrapper(img, null, imageSettings.getCatalog()))
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

    private Optional<Image> convert(ImageV4Response response) {
        return Optional.ofNullable(response)
                .map(r -> new Image(r.getCreated(), r.getDate(), r.getDescription(), r.getOs(), r.getUuid(), r.getImageSetsByProvider(), r.getOsType(),
                        r.getPackageVersions(), r.isAdvertised()));
    }
}
