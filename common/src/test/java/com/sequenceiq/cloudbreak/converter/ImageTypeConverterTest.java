package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ImageType;

import javax.persistence.AttributeConverter;

public class ImageTypeConverterTest extends DefaultEnumConverterBaseTest<ImageType> {

    @Override
    public ImageType getDefaultValue() {
        return ImageType.UNKNOWN;
    }

    @Override
    public AttributeConverter<ImageType, String> getVictim() {
        return new ImageTypeConverter();
    }
}