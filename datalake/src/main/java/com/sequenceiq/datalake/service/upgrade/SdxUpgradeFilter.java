package com.sequenceiq.datalake.service.upgrade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@Component
public class SdxUpgradeFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeFilter.class);

    public UpgradeV4Response filterSdxUpgradeResponse(SdxUpgradeRequest upgradeSdxClusterRequest, UpgradeV4Response upgradeV4Response) {
        if (CollectionUtils.isNotEmpty(upgradeV4Response.getUpgradeCandidates()) && Objects.nonNull(upgradeSdxClusterRequest)) {
            UpgradeV4Response filteredResponse =
                    new UpgradeV4Response(upgradeV4Response.getCurrent(), upgradeV4Response.getUpgradeCandidates(), upgradeV4Response.getReason());
            return filterBySdxUpgradeRequestParams(upgradeSdxClusterRequest, filteredResponse);
        }
        return upgradeV4Response;
    }

    private UpgradeV4Response filterBySdxUpgradeRequestParams(SdxUpgradeRequest upgradeSdxClusterRequest, UpgradeV4Response upgradeV4Response) {
        UpgradeV4Response filteredUpgradeResponse =
                new UpgradeV4Response(upgradeV4Response.getCurrent(), upgradeV4Response.getUpgradeCandidates(), upgradeV4Response.getReason());
        if (CollectionUtils.isNotEmpty(filteredUpgradeResponse.getUpgradeCandidates())) {
            List<ImageInfoV4Response> upgradeCandidates = filteredUpgradeResponse.getUpgradeCandidates();
            if (SdxUpgradeShowAvailableImages.LATEST_ONLY == upgradeSdxClusterRequest.getShowAvailableImages()) {
                List<ImageInfoV4Response> latestImageByRuntime = filterLatestImageByRuntime(upgradeCandidates);
                filteredUpgradeResponse.setUpgradeCandidates(latestImageByRuntime);
                LOGGER.debug("Filtering for latest image per runtimes {}", latestImageByRuntime);
            } else if (upgradeSdxClusterRequest.isDryRun()) {
                ImageInfoV4Response latestImage = upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
                filteredUpgradeResponse.setUpgradeCandidates(List.of(latestImage));
                LOGGER.debug("Choosing latest image with id {} as dry-run is specified", latestImage.getImageId());
            } else {
                filteredUpgradeResponse.setUpgradeCandidates(upgradeCandidates);
            }
        }
        return filteredUpgradeResponse;
    }

    private List<ImageInfoV4Response> filterLatestImageByRuntime(List<ImageInfoV4Response> upgradeCandidates) {
        Map<String, Map<String, Optional<ImageInfoV4Response>>> imagesByRuntime = upgradeCandidates.stream()
                .collect(Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getCdp(),
                        Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getOs(),
                                Collectors.maxBy(Comparator.comparingLong(ImageInfoV4Response::getCreated)))));
        return imagesByRuntime.values().stream()
                .map(values -> values.values().stream()
                        .flatMap(Optional::stream)
                        .toList())
                .flatMap(List::stream)
                .toList();
    }
}
