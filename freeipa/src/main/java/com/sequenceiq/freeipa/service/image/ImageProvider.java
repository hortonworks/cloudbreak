package com.sequenceiq.freeipa.service.image;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;

import java.util.Optional;

public interface ImageProvider {

    Optional<ImageWrapper> getImage(ImageSettingsRequest imageSettings, String region, String platform);
}
