package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Component
public class ImageNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageNameUtil.class);
    private static final String DEFAULT = "default";

    @Inject
    private Environment environment;

    public String determineImageName(CloudPlatform cloudPlatform, String region) {
        String platform = cloudPlatform.name().toLowerCase();
        String image = environment.getProperty(platform + "." + region);
        if (image == null) {
            image = environment.getProperty(platform + "." + DEFAULT);
        }
        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", cloudPlatform, image);
        return image;
    }
}
