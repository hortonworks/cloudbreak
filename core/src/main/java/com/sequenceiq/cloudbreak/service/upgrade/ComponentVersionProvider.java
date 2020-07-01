package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;

@Component
public class ComponentVersionProvider {

    public ImageComponentVersions getComponentVersions(Map<String, String> packageVersions, String os, String osPatchLevel) {
        return new ImageComponentVersions(
                packageVersions.get("cm"),
                packageVersions.get("cm-build-number"),
                packageVersions.get("stack"),
                packageVersions.get("cdh-build-number"),
                os,
                osPatchLevel);
    }
}
