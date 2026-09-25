package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageHyperVGenerationResolver;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Component
public class AzureImageVmTypeCompatibilityValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageVmTypeCompatibilityValidator.class);

    @Inject
    private AzureImageHyperVGenerationResolver azureImageHyperVGenerationResolver;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        Image image = cloudStack.getImage();
        HyperVGenerationTypes imageGeneration = azureImageHyperVGenerationResolver.resolve(image);
        if (imageGeneration == null) {
            LOGGER.debug("Skipping image/VM Hyper-V generation compatibility check: image [{}] has no recognised hyperVGeneration tag.",
                    image == null ? null : image.getImageName());
            return;
        }
        if (cloudStack.getGroups().isEmpty()) {
            LOGGER.debug("Skipping image/VM Hyper-V generation compatibility check: the stack has no instance groups to check against.");
            return;
        }
        Optional<String> regionName = getRegionName(ac);
        if (regionName.isEmpty()) {
            LOGGER.debug("Skipping image/VM Hyper-V generation compatibility check: no region is available in the cloud context.");
            return;
        }

        String region = regionName.get();
        Map<String, AzureVmCapabilities> capabilities;
        try {
            capabilities = ac.getParameter(AzureClient.class).getHostCapabilities(region);
        } catch (Exception e) {
            LOGGER.warn("Could not fetch VM capabilities for region '{}'. Proceeding without Hyper-V generation validation.", region, e);
            return;
        }
        if (capabilities == null) {
            LOGGER.warn("No VM capabilities were returned for region '{}'. Proceeding without Hyper-V generation validation.", region);
            return;
        }

        boolean gen2Image = HyperVGenerationTypes.V2.equals(imageGeneration);
        boolean fallbackEnabled = entitlementService.isFallbackInstanceTypeEnabled(ac.getCloudContext().getAccountId());
        List<String> offenders = collectOffenders(cloudStack, capabilities, region, gen2Image, fallbackEnabled);
        if (!offenders.isEmpty()) {
            String errorMessage = errorMessage(gen2Image, offenders);
            LOGGER.warn("{} Image: {}", errorMessage, image.getImageName());
            throw new CloudConnectorException(errorMessage);
        }
    }

    private Optional<String> getRegionName(AuthenticatedContext ac) {
        return Optional.ofNullable(ac.getCloudContext().getLocation())
                .map(Location::getRegion)
                .map(Region::value)
                .filter(StringUtils::isNotBlank);
    }

    private List<String> collectOffenders(CloudStack cloudStack, Map<String, AzureVmCapabilities> capabilities, String region, boolean gen2Image,
            boolean fallbackEnabled) {
        List<String> offenders = new ArrayList<>();
        for (Group group : cloudStack.getGroups()) {
            for (String flavor : candidateFlavors(group.getReferenceInstanceTemplate(), fallbackEnabled)) {
                AzureVmCapabilities vmCapabilities = capabilities.get(flavor);
                if (vmCapabilities == null) {
                    LOGGER.debug("No capabilities reported for instance type '{}' in region '{}', skipping its generation check.", flavor, region);
                } else if (gen2Image ? !vmCapabilities.isGen2Supported() : !vmCapabilities.isGen1Supported()) {
                    offenders.add(group.getName() + " (" + flavor + ")");
                }
            }
        }
        return offenders;
    }

    private List<String> candidateFlavors(InstanceTemplate template, boolean fallbackEnabled) {
        Stream<String> fallbacks = fallbackEnabled
                ? Optional.ofNullable(template.getFallbackInstanceTypes()).orElse(List.of()).stream()
                : Stream.empty();
        return Stream.concat(Stream.of(template.getFlavor()), fallbacks)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private String errorMessage(boolean gen2Image, List<String> offenders) {
        String imageGeneration = gen2Image ? "2" : "1";
        String vmGeneration = gen2Image ? "1" : "2";
        return String.format("The selected Azure image is Generation %s but the following instance types only support Generation %s: %s. "
                        + "Choose a Generation %s compatible instance type or a Generation %s image.",
                imageGeneration, vmGeneration, String.join(", ", offenders), imageGeneration, vmGeneration);
    }
}
