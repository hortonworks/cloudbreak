package com.sequenceiq.cloudbreak.cloud.gcp.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class GcpImageUtil {
    // GVNIC Os Feature: https://docs.cloud.google.com/compute/docs/networking/using-gvnic
    // This is needed for new generation of instance types and hyper disk support
    public static final String GVNIC = "GVNIC";

    public static final String VERSION = "v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpImageUtil.class);

    private static final String GCP_IMAGE_TYPE_PREFIX = "https://www.googleapis.com/compute/v1/projects/%s/global/images/%s";

    private static final String EMPTY_BUCKET = "";

    public String getBucket(String image) {
        if (!StringUtils.isEmpty(image) && createParts(image).length > 1) {
            String[] parts = createParts(image.replaceAll("https://storage.googleapis.com/", ""));
            return StringUtils.join(ArrayUtils.remove(parts, parts.length - 1), "/");
        } else {
            LOGGER.debug("No bucket found in source image path.");
            return EMPTY_BUCKET;
        }
    }

    public String getTarName(String image) {
        if (!StringUtils.isEmpty(image)) {
            String[] parts = createParts(image);
            return parts[parts.length - 1];
        } else {
            throw new GcpResourceException("Source image path environment variable is not well formed");
        }
    }

    private String getImageName(String image) {
        if (image.contains("/")) {
            return getTarName(image).replaceAll("(\\.tar|\\.zip|\\.gz|\\.gzip)", "").replaceAll("\\.", "-");
        }
        return image.trim();
    }

    public String getLatestImageName(String image) {
        return getImageName(image) + "-" + VERSION;
    }

    public String getGcpImageResourceName(CloudStack cloudStack) {
        String imageName = cloudStack.getParameters().get(PlatformParametersConsts.IMAGE_IDENTIFIER);
        if (StringUtils.isBlank(imageName)) {
            imageName = getImageName(cloudStack.getImage().getImageName());
        }
        return imageName;
    }

    public String getCDPImage(String projectId, CloudStack cloudStack) {
        String imageName = getGcpImageResourceName(cloudStack);
        return String.format(GCP_IMAGE_TYPE_PREFIX, projectId, imageName);
    }

    public boolean isGvnicCompatibleImage(CloudStack cloudStack) {
        return getGcpImageResourceName(cloudStack).endsWith("-" + VERSION);
    }

    private String[] createParts(String splittable) {
        return splittable.split("/");
    }
}
