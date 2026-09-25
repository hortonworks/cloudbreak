package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;

@Component
public class AzureHyperVGenerationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureHyperVGenerationProvider.class);

    @Inject
    private ImageService imageService;

    /**
     * @return "V1"/"V2" when the Azure image the stack was created from declares its Hyper-V generation, null when it does not (or the stack is not on Azure).
     * A null result means "unknown", and callers must not block on it: untagged images keep the pre-existing implicit Gen1 behaviour.
     */
    public String getImageHyperVGeneration(Stack stack) {
        if (!CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform())) {
            return null;
        }
        try {
            Image image = imageService.getImage(stack.getId());
            return AzureHyperVGenerationUtil.toGeneration(image.getTags());
        } catch (Exception e) {
            LOGGER.warn("Could not read the image of stack {}. Proceeding without Hyper-V generation validation.", stack.getId(), e);
            return null;
        }
    }
}
