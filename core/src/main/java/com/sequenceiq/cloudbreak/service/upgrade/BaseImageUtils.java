package com.sequenceiq.cloudbreak.service.upgrade;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

public final class BaseImageUtils {

    private BaseImageUtils() {
    }

    public static boolean isBaseImage(Image image) {
        String cdhBuildNumber = image.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER);
        String cmVersion = image.getPackageVersion(ImagePackageVersion.CM);
        return StringUtils.isEmpty(cdhBuildNumber) || StringUtils.isEmpty(cmVersion);
    }
}
