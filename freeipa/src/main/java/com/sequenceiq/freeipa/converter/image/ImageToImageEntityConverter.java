package com.sequenceiq.freeipa.converter.image;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageToImageEntityConverter {

    public ImageEntity convert(Image source) {
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setImageId(source.getUuid());
        imageEntity.setOs(source.getOs());
        imageEntity.setOsType(source.getOsType());
        return imageEntity;
    }
}
