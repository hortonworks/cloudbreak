package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class DistroxUpgradeImageSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxUpgradeImageSelector.class);

    public ImageInfoV4Response determineImageId(UpgradeV4Request request, List<ImageInfoV4Response> upgradeCandidates) {
        Optional<ImageInfoV4Response> image = Optional.empty();
        if (Objects.isNull(request) || request.isEmpty() || Boolean.TRUE.equals(request.getLockComponents())) {
            image = selectLatestImageFromCandidates(upgradeCandidates);
        } else if (StringUtils.isNotEmpty(request.getImageId())) {
            image = selectRequestedImage(request, upgradeCandidates);
        } else if (StringUtils.isNotEmpty(request.getRuntime())) {
            image = selectLatestImageByRuntime(upgradeCandidates, request.getRuntime());
        }
        return image.orElseThrow(() -> new BadRequestException(String.format("Invalid upgrade request, please validate the contents: %s", request)));
    }

    private Optional<ImageInfoV4Response> selectLatestImageByRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        List<ImageInfoV4Response> imagesWithMatchingRuntime = upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp())).collect(Collectors.toList());
        validateThereIsMatchingRuntime(upgradeCandidates, imagesWithMatchingRuntime, runtime);
        Optional<ImageInfoV4Response> imageInfoV4Response = imagesWithMatchingRuntime.stream().max(ImageInfoV4Response.creationBasedComparator());
        LOGGER.debug("Chosen image {} for {} runtime specified in the request", imageInfoV4Response, runtime);
        return imageInfoV4Response;
    }

    private void validateThereIsMatchingRuntime(List<ImageInfoV4Response> upgradeCandidates, List<ImageInfoV4Response> imagesWithMatchingRuntime,
            String runtime) {
        if (imagesWithMatchingRuntime.isEmpty()) {
            String availableRuntimes = collectAvailableRuntimes(upgradeCandidates);
            if (StringUtils.isEmpty(availableRuntimes)) {
                throw new BadRequestException(String.format("There is no image eligible for the cluster upgrade with runtime: %s.", runtime));
            } else {
                throw new BadRequestException(String.format("There is no image eligible for the cluster upgrade with runtime: %s. "
                        + "Please choose a runtime from the following: %s", runtime, availableRuntimes));
            }
        }
    }

    private String collectAvailableRuntimes(List<ImageInfoV4Response> upgradeCandidates) {
        return upgradeCandidates.stream()
                .map(ImageInfoV4Response::getComponentVersions)
                .map(ImageComponentVersions::getCdp)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private Optional<ImageInfoV4Response> selectRequestedImage(UpgradeV4Request request, List<ImageInfoV4Response> upgradeCandidates) {
        Optional<ImageInfoV4Response> requestedImage = upgradeCandidates.stream()
                .filter(candidate -> request.getImageId().equals(candidate.getImageId()))
                .findFirst();
        validateImageIsPresent(upgradeCandidates, requestedImage);
        LOGGER.debug("Chosen image {} as it was specified in the request", requestedImage);
        return requestedImage;
    }

    private Optional<ImageInfoV4Response> selectLatestImageFromCandidates(List<ImageInfoV4Response> upgradeCandidates) {
        Optional<ImageInfoV4Response> imageInfoV4Response = upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator());
        LOGGER.debug("Choosing latest image {} as either upgrade request is empty or lockComponents is true", imageInfoV4Response);
        return imageInfoV4Response;
    }

    private void validateImageIsPresent(List<ImageInfoV4Response> upgradeCandidates, Optional<ImageInfoV4Response> requestedImage) {
        if (requestedImage.isEmpty()) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image is not eligible for the cluster upgrade. "
                    + "Please choose an id from the following: %s", candidates));
        }
    }
}
