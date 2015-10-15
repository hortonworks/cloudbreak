package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_AMI_MAP;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AZURE_IMAGE_URI;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AZURE_RM_IMAGE;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_GCP_SOURCE_IMAGE_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_OPENSTACK_IMAGE;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component
public class ImageNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageNameUtil.class);

    @Value("${cb.azure.image.uri:}")
    private String azureImage;

    @Value("${cb.azure.rm.image.uri:}")
    private String azureRmImage;

    @Value("${cb.aws.ami.map:}")
    private String awsImage;

    @Value("${cb.openstack.image:}")
    private String openStackImage;

    @Value("${cb.gcp.source.image.path:}")
    private String gcpImage;


    public String determineImageName(CloudPlatform cloudPlatform, String region) {
        String selectedImage;
        switch (cloudPlatform) {
            case AWS:
                selectedImage = prepareAmis().get(Regions.valueOf(region).getName());
                break;
            case AZURE:
                selectedImage = selectImageName(azureImage, CB_AZURE_IMAGE_URI);
                break;
            case GCP:
                selectedImage = selectImageName(gcpImage, CB_GCP_SOURCE_IMAGE_PATH);
                break;
            case OPENSTACK:
                selectedImage = selectImageName(openStackImage, CB_OPENSTACK_IMAGE);
                break;
            case AZURE_RM:
                selectedImage = prepareAzureRmImages().get(region);
                break;
            default:
                throw new BadRequestException(String.format("Not supported cloud platform: %s", cloudPlatform));
        }

        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", cloudPlatform, selectedImage);
        return selectedImage;
    }

    private String prepareImage(Stack stack) {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        String selectedImage;
        switch (cloudPlatform) {
            case AWS:
                selectedImage = prepareAmis().get(Regions.valueOf(stack.getRegion()).getName());
                break;
            case AZURE:
                selectedImage = selectImageName(azureImage, CB_AZURE_IMAGE_URI);
                break;
            case GCP:
                selectedImage = selectImageName(gcpImage, CB_GCP_SOURCE_IMAGE_PATH);
                break;
            case OPENSTACK:
                selectedImage = selectImageName(openStackImage, CB_OPENSTACK_IMAGE);
                break;
            case AZURE_RM:
                selectedImage = prepareAzureRmImages().get(stack.getRegion());
                break;
            default:
                throw new BadRequestException(String.format("Not supported cloud platform: %s", stack.cloudPlatform()));
        }

        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", cloudPlatform, selectedImage);
        return selectedImage;
    }

    private Map<String, String> prepareAmis() {
        Map<String, String> amisMap = new HashMap<>();
        String awsImageNames = selectImageName(awsImage, CB_AWS_AMI_MAP);
        for (String s : awsImageNames.split(",")) {
            amisMap.put(s.split(":")[0], s.split(":")[1]);
        }
        return amisMap;
    }

    private Map<String, String> prepareAzureRmImages() {
        Map<String, String> azureMap = new HashMap<>();
        String azureImageNames = selectImageName(azureRmImage, CB_AZURE_RM_IMAGE);
        for (String s : azureImageNames.split(",")) {
            azureMap.put(s.split(":")[0], s.split(":")[1] + ":" + s.split(":")[2]);
        }
        return azureMap;
    }

    private String selectImageName(String imageName, String defaultImageName) {
        if (Strings.isNullOrEmpty(imageName)) {
            return defaultImageName;
        }
        return imageName;
    }

}
