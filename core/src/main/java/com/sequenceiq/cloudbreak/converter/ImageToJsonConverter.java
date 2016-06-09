package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Component
public class ImageToJsonConverter extends AbstractConversionServiceAwareConverter<Image, ImageJson> {

    @Override
    public ImageJson convert(Image source) {
        ImageJson imageJson = new ImageJson();
        imageJson.setImageName(source.getImageName());
        imageJson.setHdpVersion(source.getHdpVersion());
        return imageJson;
    }

}
