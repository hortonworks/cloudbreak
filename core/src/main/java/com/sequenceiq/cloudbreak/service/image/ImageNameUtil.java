package com.sequenceiq.cloudbreak.service.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

@Component
public class ImageNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageNameUtil.class);
    private static final String DEFAULT = "default";

    @Inject
    private Environment environment;

    public String determineImageName(Platform cloudPlatform, String region, String ambariHDPVersion) {
        String platform = cloudPlatform.value().toLowerCase();
        String image = getDefaultImage(platform, region);
        if (ambariHDPVersion != null) {
            String specificImage = getSpecificImage(platform, region, ambariHDPVersion);
            if (specificImage != null) {
                image = specificImage;
            } else {
                LOGGER.info("The specified ambari-hdp version image not found: {}", ambariHDPVersion);
            }
        }
        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", cloudPlatform, image);
        return image;
    }

    private String getDefaultImage(String platform, String region) {
        String image = getImage(platform + "." + region);
        if (image == null) {
            image = getImage(platform + "." + DEFAULT);
        }
        return image;
    }

    private String getSpecificImage(String platform, String region, String ambariHDPVersion) {
        String image = getImage(String.format("%s-%s.%s", platform, ambariHDPVersion, region));
        if (image == null) {
            image = getImage(String.format("%s-%s.%s", platform, ambariHDPVersion, DEFAULT));
        }
        return image;
    }

    private String getImage(String key) {
        return environment.getProperty(key);
    }
}
