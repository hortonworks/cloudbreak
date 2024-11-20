package com.sequenceiq.cloudbreak.service.salt;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class SaltVersionValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltVersionValidatorService.class);

    @Inject
    private ImageService imageService;

    public void validateSaltVersion(StackDto stack) {
        Set<String> gatewayInstancesWithOutdatedSaltVersion = getGatewayInstancesWithOutdatedSaltVersion(stack);
        if (!gatewayInstancesWithOutdatedSaltVersion.isEmpty()) {
            throw new BadRequestException(String.format("Salt package version is outdated on the gateway node(s). Please repair the gateway node(s) %s first!",
                    gatewayInstancesWithOutdatedSaltVersion));
        }
    }

    public Set<String> getGatewayInstancesWithOutdatedSaltVersion(StackDto stack) {
        String imageSaltVersion = getImageSaltVersion(stack);
        Set<String> gatewaysUsingOutdatedSaltVersion = new HashSet<>();
        for (InstanceMetadataView instanceMetadata : stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()) {
            String gatewaySaltVersion = getInstanceSaltVersion(instanceMetadata);
            if (StringUtils.isNoneBlank(imageSaltVersion, gatewaySaltVersion) && !imageSaltVersion.equals(gatewaySaltVersion)) {
                LOGGER.warn("Salt package version is outdated on the gateway: {} (image: {}, instance: {})",
                        instanceMetadata.getInstanceId(), imageSaltVersion, gatewaySaltVersion);
                gatewaysUsingOutdatedSaltVersion.add(instanceMetadata.getInstanceId());
            }
        }
        return gatewaysUsingOutdatedSaltVersion;
    }

    private String getImageSaltVersion(StackDto stack) {
        try {
            return imageService.getImage(stack.getId()).getPackageVersion(ImagePackageVersion.SALT);
        } catch (CloudbreakImageNotFoundException e) {
            throw new BadRequestException("Image not found", e);
        }
    }

    private String getInstanceSaltVersion(InstanceMetadataView instanceMetadata) {
        try {
            return instanceMetadata.getImage().get(Image.class).getPackageVersion(ImagePackageVersion.SALT);
        } catch (IOException e) {
            LOGGER.warn("Missing image information for instance: " + instanceMetadata.getInstanceId(), e);
            return null;
        }
    }

}
