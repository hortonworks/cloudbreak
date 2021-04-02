package com.sequenceiq.freeipa.service.image;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Optional;

@Service
public class CoreImageProvider implements ImageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreImageProvider.class);

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    @Value("${freeipa.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

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

    private Optional<Image> convert(ImageV4Response response) {
        return Optional.ofNullable(response).map(r ->
                new Image(r.getDate(), r.getDescription(), r.getOs(), r.getUuid(), r.getImageSetsByProvider(), r.getOsType()));
    }
}
