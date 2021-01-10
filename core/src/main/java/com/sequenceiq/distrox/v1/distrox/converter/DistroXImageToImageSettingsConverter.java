package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;

@Component
public class DistroXImageToImageSettingsConverter {

    public ImageSettingsV4Request convert(DistroXImageV1Request source) {
        ImageSettingsV4Request response = new ImageSettingsV4Request();
        response.setCatalog(source.getCatalog());
        response.setId(source.getId());
        return response;
    }

    public DistroXImageV1Request convert(ImageSettingsV4Request source) {
        DistroXImageV1Request response = new DistroXImageV1Request();
        response.setCatalog(source.getCatalog());
        response.setId(source.getId());
        return response;
    }

}
