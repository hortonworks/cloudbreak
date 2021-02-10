package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class UsedImagesProvider {

    @Inject
    private StackService stackService;

    public UsedImagesListV4Response getUsedImages() {
        final UsedImagesListV4Response usedImages = new UsedImagesListV4Response();

        stackService.getImagesOfAliveStacks()
                .forEach(usedImages::addImage);

        return usedImages;
    }
}
