package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;

@Component
public class AzureImageHyperVGenerationResolver {

    /**
     * @return the Hyper-V generation declared by the image catalog tags, or empty when the image does not declare one. An empty result must be treated as
     * "let the Azure SDK default decide", which keeps untagged images on their pre-existing implicit Gen1 behaviour.
     */
    public Optional<HyperVGenerationTypes> resolve(Image stackImage) {
        return Optional.ofNullable(stackImage)
                .flatMap(image -> AzureHyperVGenerationUtil.toGeneration(image.getTags()))
                .map(HyperVGenerationTypes::fromString);
    }

    /**
     * @return the Hyper-V generation of the image the instance was provisioned from, or empty when unknown. Same semantics as {@link #resolve(Image)}.
     */
    public Optional<HyperVGenerationTypes> resolve(CloudInstance instance) {
        return Optional.ofNullable(instance)
                .map(cloudInstance -> cloudInstance.getStringParameter(AzureInstanceTemplate.HYPER_V_GENERATION))
                .map(HyperVGenerationTypes::fromString);
    }
}
