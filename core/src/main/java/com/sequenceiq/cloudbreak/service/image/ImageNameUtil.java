package com.sequenceiq.cloudbreak.service.image;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;

@Component
public class ImageNameUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageNameUtil.class);
    private static final String DEFAULT = "default";

    @Inject
    private Environment environment;

    public String determineImageName(String platform, String region, String ambariVersion, String hdpVersion) {
        String image = getDefaultImage(platform, region);
        if (ambariVersion != null) {
            String specificImage = getSpecificImage(platform, region, ambariVersion, hdpVersion);
            if (specificImage != null) {
                image = specificImage;
            } else {
                LOGGER.info("The specified ambari-hdp version image not found: ambari: {} hdp: ", ambariVersion, hdpVersion);
            }
        }
        LOGGER.info("Selected VM image for CloudPlatform '{}' is: {}", platform, image);
        return image;
    }

    public String determineImageName(HDPInfo hdpInfo, String platform, String region) {
        Map<String, String> regions = hdpInfo.getImages().get(platform);
        if (regions != null) {
            String image = regions.get(region);
            return image == null ? regions.get(DEFAULT) : image;
        }
        return null;
    }

    private String getDefaultImage(String platform, String region) {
        String image = getImage(platform + "." + region);
        if (image == null) {
            image = getImage(platform + "." + DEFAULT);
        }
        return image;
    }

    private String getSpecificImage(String platform, String region, String ambariVersion, String hdpVersion) {
        String image = getImage(String.format("%s-ambari_%s-hdp_%s.%s", platform, ambariVersion, hdpVersion, region));
        if (image == null) {
            image = getImage(String.format("%s-ambari_%s-hdp_%s.%s", platform, ambariVersion, hdpVersion, DEFAULT));
        }
        return image;
    }

    private String getImage(String key) {
        return environment.getProperty(key);
    }
}
