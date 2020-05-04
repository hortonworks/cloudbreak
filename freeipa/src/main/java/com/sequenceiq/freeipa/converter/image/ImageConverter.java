package com.sequenceiq.freeipa.converter.image;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageConverter implements Converter<ImageEntity, Image> {
    @Override
    public Image convert(ImageEntity source) {
        Image image =
                new Image(source.getImageName(),
                        Map.of(InstanceGroupType.GATEWAY, Optional.ofNullable(source.getUserdata()).orElse("")),
                        source.getOs(),
                        source.getOsType(),
                        source.getImageCatalogUrl(),
                        source.getImageCatalogName(),
                        source.getImageId(),
                        Collections.emptyMap());
        return image;
    }
}
