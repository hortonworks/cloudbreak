package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class ClusterUpgradeCandidateFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeCandidateFilterService.class);

    public void filterUpgradeOptions(UpgradeV4Response upgradeOptions, UpgradeV4Request upgradeRequest, boolean datalake) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeOptions.getUpgradeCandidates();
        List<ImageInfoV4Response> filteredUpgradeCandidates;
        // We would like to upgrade to the latest available if no request params exist
        if ((Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty()) && datalake) {
            filteredUpgradeCandidates = filterDatalakeUpgradeCandidates(upgradeCandidates);
            LOGGER.info("No request param, defaulting to latest image {}", filteredUpgradeCandidates);
        } else {
            String requestImageId = upgradeRequest != null ? upgradeRequest.getImageId() : null;
            String runtime = upgradeRequest != null ? upgradeRequest.getRuntime() : null;

            // Image id param exists
            if (StringUtils.isNotEmpty(requestImageId)) {
                filteredUpgradeCandidates = validateImageId(upgradeOptions.getCurrent(), upgradeCandidates, requestImageId);
                LOGGER.info("Image successfully validated by imageId {}", requestImageId);
                // We would like to upgrade to the latest available image with given runtime
            } else if (StringUtils.isNotEmpty(runtime)) {
                filteredUpgradeCandidates = validateRuntime(upgradeCandidates, runtime);
                LOGGER.info("Image successfully filtered by runtime ({}): {}", runtime, filteredUpgradeCandidates);
            } else {
                filteredUpgradeCandidates = upgradeCandidates;
            }
        }
        upgradeOptions.setUpgradeCandidates(filteredUpgradeCandidates);
    }

    public List<ImageInfoV4Response> filterDatalakeUpgradeCandidates(List<ImageInfoV4Response> upgradeCandidates) {
        return List.of(upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow());
    }

    private List<ImageInfoV4Response> validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatibleImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatibleImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("There is no image eligible for the cluster upgrade with runtime: %s. "
                    + "Please choose a runtime from the following: %s", runtime, availableRuntimes));
        } else {
            return imagesWithMatchingRuntime.get().collect(Collectors.toList());
        }
    }

    private List<ImageInfoV4Response> validateImageId(ImageInfoV4Response currentImage, List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (!currentImage.getImageId().equalsIgnoreCase(requestImageId)
                && upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream()
                    .map(ImageInfoV4Response::getImageId)
                    .collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for the cluster upgrade. "
                    + "Please choose an id from the following image(s): %s", requestImageId, candidates));
        } else {
            return upgradeCandidates.stream()
                    .filter(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))
                    .collect(Collectors.toList());
        }
    }

}
