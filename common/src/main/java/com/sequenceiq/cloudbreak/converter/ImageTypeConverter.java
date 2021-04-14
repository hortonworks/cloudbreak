package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ImageType;

public class ImageTypeConverter extends DefaultEnumConverter<ImageType> {

    @Override
    public ImageType getDefault() {
        return ImageType.UNKNOWN;
    }
}
