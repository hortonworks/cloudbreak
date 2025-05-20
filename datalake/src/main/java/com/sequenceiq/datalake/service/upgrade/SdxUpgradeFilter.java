package com.sequenceiq.datalake.service.upgrade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@Component
public class SdxUpgradeFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeFilter.class);

    private static final Versioned INVALID_TARGET_VERSION_MDL = () -> "7.2.18";

    @Inject
    private EntitlementService entitlementService;

    public UpgradeV4Response filterSdxUpgradeResponse(SdxUpgradeRequest upgradeSdxClusterRequest, UpgradeV4Response upgradeV4Response,
        SdxClusterShape clusterShape) {
        if (CollectionUtils.isNotEmpty(upgradeV4Response.getUpgradeCandidates()) && Objects.nonNull(upgradeSdxClusterRequest)) {
            UpgradeV4Response filteredResponse =
                new UpgradeV4Response(upgradeV4Response.getCurrent(), upgradeV4Response.getUpgradeCandidates(), upgradeV4Response.getReason());
            return filterBySdxUpgradeRequestParams(upgradeSdxClusterRequest, filteredResponse, clusterShape);
        }
        return upgradeV4Response;
    }

    private UpgradeV4Response filterBySdxUpgradeRequestParams(SdxUpgradeRequest upgradeSdxClusterRequest, UpgradeV4Response upgradeV4Response,
        SdxClusterShape clusterShape) {
        UpgradeV4Response filteredUpgradeResponse =
            new UpgradeV4Response(upgradeV4Response.getCurrent(), upgradeV4Response.getUpgradeCandidates(), upgradeV4Response.getReason());
        if (CollectionUtils.isNotEmpty(filteredUpgradeResponse.getUpgradeCandidates())) {
            List<ImageInfoV4Response> upgradeCandidates = filterImagesByShape(filteredUpgradeResponse, clusterShape);
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

    private List<ImageInfoV4Response> filterImagesByShape(UpgradeV4Response response, SdxClusterShape clusterShape) {
        if (clusterShape != SdxClusterShape.MEDIUM_DUTY_HA
            || entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(ThreadBasedUserCrnProvider.getAccountId())) {
            return response.getUpgradeCandidates();
        } else {
            return response.getUpgradeCandidates()
                .stream()
                .filter(candidate -> !isVersionNewerOrEqualThanLimitedMDLRuntime(candidate.getComponentVersions().getCdp()))
                .collect(Collectors.toList());
        }
    }

    private boolean isVersionNewerOrEqualThanLimitedMDLRuntime(String runtime) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, INVALID_TARGET_VERSION_MDL) > -1;
    }
}
