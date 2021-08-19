package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ParcelInfoResponse;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@Component
public class ComponentVersionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentVersionProvider.class);

    private static final String GBN = "_gbn";

    public ImageComponentVersions getComponentVersions(Map<String, String> packageVersions, String os, String osPatchLevel) {
        return new ImageComponentVersions(
                packageVersions.get(ImagePackageVersion.CM.getKey()),
                packageVersions.get(ImagePackageVersion.CM_BUILD_NUMBER.getKey()),
                packageVersions.get(ImagePackageVersion.STACK.getKey()),
                packageVersions.get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey()),
                os,
                osPatchLevel,
                getParcelInfoResponse(packageVersions));
    }

    private List<ParcelInfoResponse> getParcelInfoResponse(Map<String, String> packageVersions) {
        List<ParcelInfoResponse> parcelInfoResponses = packageVersions.entrySet()
                .stream()
                .filter(this::imagePackageVersionExists)
                .map(entry -> new ParcelInfoResponse(
                        ImagePackageVersion.getByKey(entry.getKey()).get().getDisplayName(),
                        entry.getValue(),
                        packageVersions.get(entry.getKey() + GBN)))
                .collect(Collectors.toList());
        LOGGER.debug("Package version on the image: {}, transformed parcel versions are: {}", packageVersions, parcelInfoResponses);
        return  parcelInfoResponses;
    }

    private boolean imagePackageVersionExists(Map.Entry<String, String> entry) {
        return Arrays.stream(ImagePackageVersion.values())
                .filter(ImagePackageVersion::hasProperDisplayName)
                .anyMatch(imagePackageVersion -> imagePackageVersion.getKey().equals(entry.getKey()));
    }

}
