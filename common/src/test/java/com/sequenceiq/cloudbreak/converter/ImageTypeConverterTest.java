package com.sequenceiq.cloudbreak.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.ImageType;

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