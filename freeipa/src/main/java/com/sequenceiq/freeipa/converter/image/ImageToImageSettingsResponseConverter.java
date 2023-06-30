package com.sequenceiq.freeipa.converter.image;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageToImageSettingsResponseConverter implements Converter<ImageEntity, ImageSettingsResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageToImageSettingsResponseConverter.class);

    @Inject
    private ImageToImageEntityConverter imageEntityConverter;

    public ImageSettingsResponse convert(ImageEntity source) {
        ImageSettingsResponse response = new ImageSettingsResponse();
        response.setCatalog(source.getImageCatalogUrl());
        response.setId(source.getImageId());
        response.setOs(source.getOs());
        response.setLdapAgentVersion(source.getLdapAgentVersion());
        return response;
    }

    public ImageSettingsResponse convert(com.sequenceiq.cloudbreak.cloud.model.Image source) {
        if (source != null) {
            ImageSettingsResponse response = new ImageSettingsResponse();
            response.setId(source.getImageId());
            response.setOs(source.getOs());
            response.setCatalog(StringUtils.isNotEmpty(source.getImageCatalogUrl()) ? source.getImageCatalogUrl() : source.getImageCatalogName());
            response.setLdapAgentVersion(imageEntityConverter.extractLdapAgentVersion(source));
            return response;
        } else {
            LOGGER.debug("Source image is null");
            return null;
        }
    }
}
