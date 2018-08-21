package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;

@Component
public class ImageToImageJsonConverter extends AbstractConversionServiceAwareConverter<Image, ImageJson> {

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    public ImageJson convert(Image source) {
        ImageJson imageJson = new ImageJson();
        imageJson.setImageName(source.getImageName());
        imageJson.setImageCatalogUrl(Strings.isNullOrEmpty(source.getImageCatalogUrl())
                ? imageCatalogService.getImageDefaultCatalogUrl() : source.getImageCatalogUrl());
        imageJson.setImageCatalogName(Strings.isNullOrEmpty(source.getImageCatalogName())
                ? imageCatalogService.getDefaultImageCatalogName() : source.getImageCatalogName());
        imageJson.setImageId(Strings.isNullOrEmpty(source.getImageId())
                ? null : source.getImageId());
        return imageJson;
    }

}
