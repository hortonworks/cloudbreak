package com.sequenceiq.freeipa.service.image;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;

public interface ImageProvider {

    Optional<ImageWrapper> getImage(String accountId, ImageSettingsRequest imageSettings, String region, String platformString);

    List<ImageWrapper> getImages(String accountId, ImageSettingsRequest imageSettings, String region, String platformString);
}
