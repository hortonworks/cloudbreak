package com.sequenceiq.cloudbreak.service.image;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;

@Component
public class ImageUtil {

    public boolean isArm64Image(Image image) {
        return !Objects.isNull(image) && Architecture.fromStringWithFallback(image.getArchitecture()) == Architecture.ARM64;
    }
}
