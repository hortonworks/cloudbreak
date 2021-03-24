package com.sequenceiq.datalake.service.upgrade;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@Component
public class SdxRuntimeUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRuntimeUpgradeService.class);

    private static final long WORKSPACE_ID = 0L;

    @Value("${sdx.paywall.url}")
    private String paywallUrl;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private SdxUpgradeClusterConverter sdxUpgradeClusterConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private PaywallAccessChecker paywallAccessChecker;

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    public SdxUpgradeResponse checkForUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeSdxClusterRequest, String accountId) {
        return checkForSdxUpgradeResponse(userCrn, upgradeSdxClusterRequest, clusterName, accountId);
    }

    public SdxUpgradeResponse checkForUpgradeByCrn(String userCrn, String crn, SdxUpgradeRequest upgradeSdxClusterRequest, String accountId) {
        String clusterName = getClusterName(userCrn, crn);
        return checkForSdxUpgradeResponse(userCrn, upgradeSdxClusterRequest, clusterName, accountId);
    }

    private String getClusterName(String userCrn, String clusterCrn) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        return cluster.getClusterName();
    }

    public SdxUpgradeResponse triggerUpgradeByName(String userCrn, String clusterName, SdxUpgradeRequest upgradeRequest, String accountId) {
        SdxCluster cluster = sdxService.getByNameInAccount(userCrn, clusterName);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByName(userCrn, clusterName, upgradeRequest, accountId);
        List<ImageInfoV4Response> imageInfoV4Responses = validateUpgradeCandidates(clusterName, sdxUpgradeResponse);
        return initSdxUpgrade(userCrn, imageInfoV4Responses, upgradeRequest, cluster);
    }

    public SdxUpgradeResponse triggerUpgradeByCrn(String userCrn, String clusterCrn, SdxUpgradeRequest upgradeRequest, String accountId) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        SdxUpgradeResponse sdxUpgradeResponse = checkForUpgradeByCrn(userCrn, clusterCrn, upgradeRequest, accountId);
        List<ImageInfoV4Response> imageInfoV4Responses = validateUpgradeCandidates(cluster.getClusterName(), sdxUpgradeResponse);
        return initSdxUpgrade(userCrn, imageInfoV4Responses, upgradeRequest, cluster);
    }

    public boolean isRuntimeUpgradeEnabled(String userCrn) {
        String accountId = sdxService.getAccountIdFromCrn(userCrn);
        return entitlementService.runtimeUpgradeEnabled(accountId);
    }

    private boolean isInternalRepoAllowedForUpgrade(String userCrn) {
        String accountId = sdxService.getAccountIdFromCrn(userCrn);
        return entitlementService.isInternalRepositoryForUpgradeAllowed(accountId);
    }

    private SdxUpgradeResponse checkForSdxUpgradeResponse(String userCrn, SdxUpgradeRequest upgradeSdxClusterRequest,
            String clusterName, String accountId) {
        verifyRuntimeUpgradeEntitlement(userCrn, upgradeSdxClusterRequest);
        UpgradeV4Response upgradeV4Response = ThreadBasedUserCrnProvider
                .doAsInternalActor(() -> stackV4Endpoint.checkForClusterUpgradeByName(WORKSPACE_ID, clusterName,
                        sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(upgradeSdxClusterRequest), accountId));
        filterSdxUpgradeResponse(upgradeSdxClusterRequest, upgradeV4Response);
        return sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(upgradeV4Response);
    }

    @VisibleForTesting
    void filterSdxUpgradeResponse(SdxUpgradeRequest upgradeSdxClusterRequest, UpgradeV4Response upgradeV4Response) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeV4Response.getUpgradeCandidates();
        if (CollectionUtils.isNotEmpty(upgradeCandidates) && Objects.nonNull(upgradeSdxClusterRequest)) {
            if (SdxUpgradeShowAvailableImages.LATEST_ONLY == upgradeSdxClusterRequest.getShowAvailableImages()) {
                Map<String, Optional<ImageInfoV4Response>> latestImageByRuntime = upgradeCandidates.stream()
                        .collect(Collectors.groupingBy(imageInfoV4Response -> imageInfoV4Response.getComponentVersions().getCdp(),
                                Collectors.maxBy(Comparator.comparingLong(ImageInfoV4Response::getCreated))));
                List<ImageInfoV4Response> latestImages = latestImageByRuntime.values()
                        .stream()
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList());
                upgradeV4Response.setUpgradeCandidates(latestImages);
                LOGGER.debug("Filtering for latest image per runtimes {}", latestImageByRuntime.keySet());

            } else if (upgradeSdxClusterRequest.isDryRun()) {
                ImageInfoV4Response latestImage = upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
                upgradeV4Response.setUpgradeCandidates(List.of(latestImage));
                LOGGER.debug("Choosing latest image with id {} as dry-run is specified", latestImage.getImageId());

            }
        }
    }

    private SdxUpgradeResponse initSdxUpgrade(String userCrn, List<ImageInfoV4Response> upgradeCandidates, SdxUpgradeRequest request, SdxCluster cluster) {
        verifyPaywallAccess(userCrn, request);
        String imageId = determineImageId(request, upgradeCandidates);
        FlowIdentifier flowIdentifier = triggerDatalakeUpgradeFlow(imageId, cluster, shouldReplaceVmsAfterUpgrade(request));
        String message = getMessage(imageId);
        return new SdxUpgradeResponse(message, flowIdentifier);
    }

    private void verifyRuntimeUpgradeEntitlement(String userCrn, SdxUpgradeRequest upgradeSdxClusterRequest) {
        if (upgradeSdxClusterRequest != null && !Boolean.TRUE.equals(upgradeSdxClusterRequest.getLockComponents()) && !isRuntimeUpgradeEnabled(userCrn)) {
            throw new BadRequestException("Runtime upgrade feature is not enabled");
        }
    }

    private void verifyPaywallAccess(String userCrn, SdxUpgradeRequest upgradeRequest) {
        if (upgradeRequest != null && !Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            if (!isInternalRepoAllowedForUpgrade(userCrn)) {
                verifyCMLicenseValidity(userCrn);
            } else {
                LOGGER.info("Internal repo is allowed for upgrade, skip CM license validation");
            }
        }
    }

    private void verifyCMLicenseValidity(String userCrn) {
        LOGGER.info("Verify if the CM license is valid to authenticate to {}", paywallUrl);
        JsonCMLicense license = clouderaManagerLicenseProvider.getLicense(userCrn);
        paywallAccessChecker.checkPaywallAccess(license, paywallUrl);
    }

    private FlowIdentifier triggerDatalakeUpgradeFlow(String imageId, SdxCluster cluster, SdxUpgradeReplaceVms replaceVms) {
        MDCBuilder.buildMdcContext(cluster);
        return sdxReactorFlowManager.triggerDatalakeRuntimeUpgradeFlow(cluster, imageId, replaceVms);
    }

    private String getMessage(String imageId) {
        return messagesService.getMessage(ResourceEvent.DATALAKE_UPGRADE.getMessage(), Collections.singletonList(imageId));
    }

    private String determineImageId(SdxUpgradeRequest upgradeRequest, List<ImageInfoV4Response> upgradeCandidates) {
        String imageId;

        if (Objects.isNull(upgradeRequest) || upgradeRequest.isEmpty() || Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            ImageInfoV4Response imageInfoV4Response = upgradeCandidates.stream().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
            imageId = imageInfoV4Response.getImageId();
            LOGGER.debug("Choosing latest image with id {} as either upgrade request is empty or lockComponents is true", imageId);
        } else {
            String requestImageId = upgradeRequest.getImageId();
            String runtime = upgradeRequest.getRuntime();

            if (StringUtils.isNotEmpty(requestImageId)) {
                imageId = validateImageId(upgradeCandidates, requestImageId);
                LOGGER.debug("Chosen image with id {} as it was specified in the request", imageId);
            } else if (StringUtils.isNotEmpty(runtime)) {
                imageId = validateRuntime(upgradeCandidates, runtime);
                LOGGER.debug("Chosen image with id {} for {} runtime specified in the request", imageId, runtime);
            } else {
                throw new BadRequestException(String.format("Invalid upgrade request, please validate the contents: %s", upgradeRequest));
            }
        }
        return imageId;
    }

    private List<ImageInfoV4Response> validateUpgradeCandidates(String clusterName, SdxUpgradeResponse upgradeResponse) {
        List<ImageInfoV4Response> upgradeCandidates = upgradeResponse.getUpgradeCandidates();
        if (StringUtils.isNotEmpty(upgradeResponse.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                    upgradeResponse.getReason()));
        } else if (CollectionUtils.isEmpty(upgradeCandidates)) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", clusterName));
        }
        return upgradeCandidates;
    }

    private String validateRuntime(List<ImageInfoV4Response> upgradeCandidates, String runtime) {
        String imageId;
        Supplier<Stream<ImageInfoV4Response>> imagesWithMatchingRuntime = () -> upgradeCandidates.stream().filter(
                imageInfoV4Response -> runtime.equals(imageInfoV4Response.getComponentVersions().getCdp()));
        boolean hasCompatbileImageWithRuntime = imagesWithMatchingRuntime.get().anyMatch(e -> true);
        if (!hasCompatbileImageWithRuntime) {
            String availableRuntimes = upgradeCandidates
                    .stream()
                    .map(ImageInfoV4Response::getComponentVersions)
                    .map(ImageComponentVersions::getCdp)
                    .distinct()
                    .collect(Collectors.joining(","));
            String errorMessage;
            if (StringUtils.isEmpty(availableRuntimes)) {
                errorMessage = String.format("There is no image eligible for the cluster upgrade with runtime: %s.", runtime);
            } else {
                errorMessage = String.format("There is no image eligible for the cluster upgrade with runtime: %s. "
                        + "Please choose a runtime from the following: %s", runtime, availableRuntimes);
            }
            throw new BadRequestException(errorMessage);
        } else {
            ImageInfoV4Response imageInfoV4Response = imagesWithMatchingRuntime.get().max(ImageInfoV4Response.creationBasedComparator()).orElseThrow();
            imageId = imageInfoV4Response.getImageId();
        }
        return imageId;
    }

    private String validateImageId(List<ImageInfoV4Response> upgradeCandidates, String requestImageId) {
        if (upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(requestImageId))) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for the cluster upgrade. "
                    + "Please choose an id from the following: %s", requestImageId, candidates));
        }
        return requestImageId;
    }

    private SdxUpgradeReplaceVms shouldReplaceVmsAfterUpgrade(SdxUpgradeRequest upgradeRequest) {
        SdxUpgradeReplaceVms replaceVms = Optional.ofNullable(upgradeRequest)
                .map(SdxUpgradeRequest::getReplaceVms)
                .orElse(SdxUpgradeReplaceVms.ENABLED);
        LOGGER.debug("VM-s replacement after the upgrade process is {}", replaceVms.name());
        return replaceVms;
    }

    public boolean isOsUpgrade(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }
}
