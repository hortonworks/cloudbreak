package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ParcelInfoResponse;

@Component
public class ImageComponentVersionsComparator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageComponentVersionsComparator.class);

    public boolean containsSamePackages(ImageComponentVersions image1, ImageComponentVersions image2) {
        boolean result = Objects.equals(image1.getCdp(), image2.getCdp()) &&
                Objects.equals(image1.getCdpGBN(), image2.getCdpGBN()) &&
                Objects.equals(image1.getCm(), image2.getCm()) &&
                Objects.equals(image1.getCmGBN(), image2.getCmGBN()) &&
                compareParcelVersions(image1.getParcelInfoResponseList(), image2.getParcelInfoResponseList());
        LOGGER.debug("Comparing image component versions: {}, {}, contains same packages: {}", image1, image2, result);
        return result;
    }

    private boolean compareParcelVersions(List<ParcelInfoResponse> parcelList1, List<ParcelInfoResponse> parcelList2) {
        return Objects.equals(getParcelNameAndBuildNumber(parcelList1), getParcelNameAndBuildNumber(parcelList2));
    }

    private Map<String, String> getParcelNameAndBuildNumber(List<ParcelInfoResponse> parcelList) {
        return parcelList.stream()
                .filter(parcel -> Objects.nonNull(parcel.getBuildNumber()))
                .collect(Collectors.toMap(ParcelInfoResponse::getName, ParcelInfoResponse::getBuildNumber));
    }
}
