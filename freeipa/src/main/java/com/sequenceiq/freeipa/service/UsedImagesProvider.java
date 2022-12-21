package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UsedImagesProvider {

    @Inject
    private StackService stackService;

    public UsedImagesListV1Response getUsedImages(Integer thresholdInDays) {
        final UsedImagesListV1Response usedImages = new UsedImagesListV1Response();

        stackService.getImagesOfAliveStacks(thresholdInDays).stream()
                .map(ImageEntity::getImageId)
                .forEach(usedImages::addImage);

        return usedImages;
    }
}
