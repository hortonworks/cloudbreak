package com.sequenceiq.freeipa.converter.image;

import java.util.Map;
import java.util.Optional;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageConverter implements Converter<ImageEntity, Image> {

    @Override
    public Image convert(ImageEntity source) {
        return new Image(source.getImageName(),
                Map.of(
                        InstanceGroupType.GATEWAY, Optional.ofNullable(source.getUserdataWrapper()).orElse(""),
                        InstanceGroupType.CORE, Optional.ofNullable(source.getUserdataWrapper()).orElse("")
                ),
                source.getOs(),
                source.getOsType(),
                Architecture.fromStringWithFallback(source.getArchitecture()).getName(),
                source.getImageCatalogUrl(),
                source.getImageCatalogName(),
                source.getImageId(),
                StringUtils.isNotBlank(source.getSourceImage()) ?
                        Map.of(ImagePackageVersion.SOURCE_IMAGE.getKey(), source.getSourceImage(),
                                ImagePackageVersion.IMDS_VERSION.getKey(), Strings.nullToEmpty(source.getImdsVersion()),
                                ImagePackageVersion.SALT.getKey(), Strings.nullToEmpty(source.getSaltVersion())) :
                        Map.of(ImagePackageVersion.IMDS_VERSION.getKey(), Strings.nullToEmpty(source.getImdsVersion()),
                                ImagePackageVersion.SALT.getKey(), Strings.nullToEmpty(source.getSaltVersion())),
                source.getDate(),
                null);
    }
}
