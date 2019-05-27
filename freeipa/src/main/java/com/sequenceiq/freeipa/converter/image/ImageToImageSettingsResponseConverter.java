package com.sequenceiq.freeipa.converter.image;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.entity.Image;

@Component
public class ImageToImageSettingsResponseConverter implements Converter<Image, ImageSettingsResponse> {

    public ImageSettingsResponse convert(Image source) {
        ImageSettingsResponse response = new ImageSettingsResponse();
        response.setCatalog(source.getImageCatalogUrl());
        response.setId(source.getImageId());
        response.setOs(source.getOs());
        return response;
    }
}
