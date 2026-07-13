package com.sequenceiq.freeipa.converter.image;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;

@Component
public class ImageConverter implements Converter<ImageEntity, Image> {

    @Inject
    private ImageToImageEntityConverter imageToImageEntityConverter;

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
                null,
                null);
    }

    public Image convertWithoutUserdata(ImageEntity source) {
        Image image = convert(source);
        image.setUserdata(Map.of());
        return image;
    }

    public Image convert(Pair<ImageWrapper, String> source) {
        ImageWrapper imageWrapper = source.getLeft();
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image = imageWrapper.getImage();
        return new Image(source.getRight(),
                Map.of(),
                image.getOs(),
                image.getOsType(),
                Architecture.fromStringWithFallback(image.getArchitecture()).getName(),
                imageWrapper.getCatalogUrl(),
                imageWrapper.getCatalogName(),
                image.getUuid(),
                StringUtils.isNotBlank(image.getSourceImageId()) ?
                        Map.of(ImagePackageVersion.SOURCE_IMAGE.getKey(), image.getSourceImageId(),
                                ImagePackageVersion.IMDS_VERSION.getKey(), Strings.nullToEmpty(imageToImageEntityConverter.extractImdsVersion(image)),
                                ImagePackageVersion.SALT.getKey(), Strings.nullToEmpty(imageToImageEntityConverter.extractSaltVersion(image))) :
                        Map.of(ImagePackageVersion.IMDS_VERSION.getKey(), Strings.nullToEmpty(imageToImageEntityConverter.extractImdsVersion(image)),
                                ImagePackageVersion.SALT.getKey(), Strings.nullToEmpty(imageToImageEntityConverter.extractSaltVersion(image))),
                image.getDate(),
                image.getCreated(),
                image.getTags());
    }
}
