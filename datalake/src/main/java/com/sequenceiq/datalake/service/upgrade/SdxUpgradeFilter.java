package com.sequenceiq.datalake.service.upgrade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    public UpgradeV4Response filterSdxUpgradeResponse(String accountId, String clusterName, SdxUpgradeRequest upgradeSdxClusterRequest,
            UpgradeV4Response upgradeV4Response) {
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
                Map<String, Optional<ImageInfoV4Response>> latestImageByRuntime = upgradeCandidates.stream()
                        .collect(Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getCdp(),
                                Collectors.maxBy(Comparator.comparingLong(ImageInfoV4Response::getCreated))));
                List<ImageInfoV4Response> latestImages = latestImageByRuntime.values()
                        .stream()
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList());
                filteredUpgradeResponse.setUpgradeCandidates(latestImages);
                LOGGER.debug("Filtering for latest image per runtimes {}", latestImageByRuntime.keySet());
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

    private void updatePatchUpgradeReasonIfNeeded(String accountId, String clusterName, UpgradeV4Response upgradeV4Response,
            SdxUpgradeRequest sdxUpgradeRequest, String currentCdpVersion) {
        String targetRuntime = sdxUpgradeRequest.getRuntime();
        if (StringUtils.isNotEmpty(targetRuntime) && !currentCdpVersion.equals(targetRuntime)) {
            String message = String.format(
                    "Only patch upgrade is enabled in account [%s], it is not possible to upgrade from [%s] to [%s] runtime on [%s] cluster",
                    accountId, currentCdpVersion, targetRuntime, clusterName);
            LOGGER.info(message);
            upgradeV4Response.setReason(message);
        }
        if (StringUtils.isNotEmpty(sdxUpgradeRequest.getImageId()) && upgradeV4Response.getUpgradeCandidates().isEmpty()) {
            String message = String.format("Only patch upgrade is enabled in account [%s], the target image [%s] is not a patch upgrade." +
                            " The version of target runtime has to be the same as the current one on [%s] cluster, current runtime: [%s]",
                    accountId, sdxUpgradeRequest.getImageId(), clusterName, currentCdpVersion);
            LOGGER.info(message);
            upgradeV4Response.setReason(message);
        }
    }

}
