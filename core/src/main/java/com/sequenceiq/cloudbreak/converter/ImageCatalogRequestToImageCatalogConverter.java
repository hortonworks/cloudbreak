package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.CachedImageCatalogProvider;

@Component
public class ImageCatalogRequestToImageCatalogConverter extends AbstractConversionServiceAwareConverter<ImageCatalogRequest, ImageCatalog> {

    @Inject
    private CachedImageCatalogProvider cachedImageCatalogProvider;

    @Override
    public ImageCatalog convert(ImageCatalogRequest source) {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(source.getUrl());
        imageCatalog.setImageCatalogName(source.getName());

        validateImageCatalog(imageCatalog);

        return imageCatalog;
    }

    private void validateImageCatalog(ImageCatalog imageCatalog) {
        try {
            cachedImageCatalogProvider.getImageCatalogV2(imageCatalog.getImageCatalogUrl());
        } catch (CloudbreakImageCatalogException e) {
            throw new BadRequestException("Failed to download and parse image catalog JSON from the given URL: " + e.getMessage());
        } finally {
            cachedImageCatalogProvider.evictImageCatalogCache(imageCatalog.getImageCatalogUrl());
        }
    }
}
