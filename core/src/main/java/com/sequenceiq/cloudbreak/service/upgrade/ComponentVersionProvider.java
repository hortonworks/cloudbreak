package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@Component
public class ComponentVersionProvider {

    public ImageComponentVersions getComponentVersions(Map<String, String> packageVersions, String os, String osPatchLevel) {
        return new ImageComponentVersions(
                packageVersions.get(ImagePackageVersion.CM.getKey()),
                packageVersions.get(ImagePackageVersion.CM_BUILD_NUMBER.getKey()),
                packageVersions.get(ImagePackageVersion.STACK.getKey()),
                packageVersions.get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey()),
                os,
                osPatchLevel);
    }
}
