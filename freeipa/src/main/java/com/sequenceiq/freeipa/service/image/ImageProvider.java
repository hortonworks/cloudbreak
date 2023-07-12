package com.sequenceiq.freeipa.service.image;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.freeipa.dto.ImageWrapper;

public interface ImageProvider {

    Optional<ImageWrapper> getImage(FreeIpaImageFilterSettings imageFilterParams);

    Optional<ImageWrapper> getImage(FreeIpaImageFilterSettings imageFilterParams, String accountId);

    List<ImageWrapper> getImages(FreeIpaImageFilterSettings imageFilterParams);
}
