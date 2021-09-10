package com.sequenceiq.freeipa.service.image;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;

public interface ImageProvider {

    Optional<ImageWrapper> getImage(ImageSettingsRequest imageSettings, String region, String platform);

    List<ImageWrapper> getImages(ImageSettingsRequest imageSettings, String region, String platform);
}
