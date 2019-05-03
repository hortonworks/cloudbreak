package com.sequenceiq.freeipa.converter.cloud;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.freeipa.entity.Image;

@Component
public class ImageConverter implements Converter<Image, com.sequenceiq.cloudbreak.cloud.model.Image> {
    @Override
    public com.sequenceiq.cloudbreak.cloud.model.Image convert(Image source) {
        com.sequenceiq.cloudbreak.cloud.model.Image image =
                new com.sequenceiq.cloudbreak.cloud.model.Image(source.getImageName(),
                        Map.of(InstanceGroupType.GATEWAY, source.getUserdata(),
                                InstanceGroupType.CORE, source.getUserdata()),
                        source.getOs(),
                        source.getOsType(),
                        source.getImageCatalogUrl(),
                        source.getImageCatalogName(),
                        source.getImageId(),
                        Collections.emptyMap());
        return image;
    }
}
