package com.sequenceiq.freeipa.service.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.GenerateImageCatalogResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ImageCatalogGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogGeneratorService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    public GenerateImageCatalogResponse generate(String environmentCrn, String accountId) {
        final Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);

        LOGGER.info("Generating image catalog for environment {} stack {}", environmentCrn, stack.getId());
        final ImageCatalog imageCatalog = imageService.generateImageCatalogForStack(stack);

        final GenerateImageCatalogResponse response = new GenerateImageCatalogResponse();
        response.setImageCatalog(imageCatalog);
        return response;
    }
}
