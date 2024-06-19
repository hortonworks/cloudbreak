package com.sequenceiq.cloudbreak.service.image;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageUtil {

    public boolean isArm64Image(Image image) {
        return !Objects.isNull(image) && !Objects.isNull(image.getTags()) && "arm64".equals(image.getTags().get("platform"));
    }
}
