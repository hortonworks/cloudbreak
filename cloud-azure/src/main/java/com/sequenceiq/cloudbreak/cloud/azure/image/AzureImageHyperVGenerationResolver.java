package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;

@Component
public class AzureImageHyperVGenerationResolver {

    /**
     * @return the Hyper-V generation declared by the image catalog tags, or null when the image does not declare one. A null result must be treated as
     * "let the Azure SDK default decide", which keeps untagged images on their pre-existing implicit Gen1 behaviour.
     */
    public HyperVGenerationTypes resolve(Image stackImage) {
        if (stackImage == null) {
            return null;
        }
        return toHyperVGenerationType(AzureHyperVGenerationUtil.toGeneration(stackImage.getTags()));
    }

    /**
     * @return the Hyper-V generation of the image the instance was provisioned from, or null when unknown. Same null semantics as {@link #resolve(Image)}.
     */
    public HyperVGenerationTypes resolve(CloudInstance instance) {
        if (instance == null) {
            return null;
        }
        return toHyperVGenerationType(instance.getStringParameter(AzureInstanceTemplate.HYPER_V_GENERATION));
    }

    private HyperVGenerationTypes toHyperVGenerationType(String generation) {
        return generation == null ? null : HyperVGenerationTypes.fromString(generation);
    }
}
